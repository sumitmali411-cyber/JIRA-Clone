#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# start-zip.sh  — Start ALL services using Keycloak ZIP (no Docker for Keycloak)
#
# What this starts:
#   [ZIP]     Keycloak 24        → http://localhost:8180  (from C:\comic\keycloak-24.0.0.zip)
#   [Docker]  DevSync MySQL      → localhost:3309          (still uses Docker for MySQL)
#   [Local]   JIRA Clone backend → http://localhost:8080
#   [Local]   DevSync backend    → http://localhost:9090
#   [Local]   JIRA Clone UI      → http://localhost:4200
#   [Local]   DevSync UI         → http://localhost:4300
#
# Keycloak ZIP: C:\comic\keycloak-24.0.0.zip
# Extracted to: C:\keycloak\keycloak-24.0.0\
#
# Prerequisites:
#   - Java 17+ installed (Keycloak ZIP needs it)
#   - Docker Desktop running (for DevSync MySQL only)
#   - MySQL running locally at port 3306 (for JIRA Clone)
#   - Maven, Node.js installed
# ─────────────────────────────────────────────────────────────────────────────
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JIRA_DIR="$SCRIPT_DIR"
PM_DIR="/c/Users/sumit/projects/Project-management-application"
LOG_DIR="/tmp/jira-logs"
mkdir -p "$LOG_DIR"

# Keycloak ZIP paths
KC_ZIP="/c/comic/keycloak-24.0.0.zip"
KC_EXTRACT_DIR="/c/keycloak"
KC_HOME="$KC_EXTRACT_DIR/keycloak-24.0.0"
KC_BIN="$KC_HOME/bin/kc.bat"
KC_PORT=8180

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()  { echo -e "${GREEN}[START]${NC} $1"; }
warn()  { echo -e "${YELLOW}[ WAIT]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; }

wait_for_http() {
  local url=$1 label=$2 max=${3:-80}
  warn "Waiting for $label ..."
  for i in $(seq 1 $max); do
    if curl -sf "$url" > /dev/null 2>&1; then
      info "$label is ready"
      return 0
    fi
    sleep 3
  done
  error "$label did not become ready — check $LOG_DIR/keycloak.log"
  return 1
}

configure_keycloak_realm() {
  info "Configuring Keycloak realm (registration + theme) ..."

  TOKEN=$(curl -sf -X POST "http://localhost:${KC_PORT}/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=admin&password=admin&grant_type=password&client_id=admin-cli" \
    | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

  if [ -z "$TOKEN" ]; then
    warn "Could not get admin token — configure realm manually at http://localhost:${KC_PORT}"
    return
  fi

  REALM_EXISTS=$(curl -sf -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $TOKEN" \
    "http://localhost:${KC_PORT}/admin/realms/jira-clone" || echo "000")

  if [ "$REALM_EXISTS" = "200" ]; then
    curl -sf -X PUT "http://localhost:${KC_PORT}/admin/realms/jira-clone" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "registrationAllowed": true,
        "registrationEmailAsUsername": false,
        "loginTheme": "jira-clone",
        "rememberMe": true,
        "resetPasswordAllowed": true
      }' && info "Realm 'jira-clone' updated: registration ENABLED, theme 'jira-clone'"
  else
    curl -sf -X POST "http://localhost:${KC_PORT}/admin/realms" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "realm": "jira-clone",
        "enabled": true,
        "registrationAllowed": true,
        "registrationEmailAsUsername": false,
        "loginTheme": "jira-clone",
        "rememberMe": true,
        "resetPasswordAllowed": true,
        "clients": [{
          "clientId": "jira-clone-app",
          "enabled": true,
          "publicClient": true,
          "redirectUris": ["http://localhost:4200/*"],
          "webOrigins": ["http://localhost:4200"]
        }]
      }' && info "Realm 'jira-clone' created with registration enabled"
  fi
}

# ── Step 1: Check Java ────────────────────────────────────────────────────────
info "Checking Java ..."
if ! java -version > /dev/null 2>&1; then
  error "Java not found. Keycloak ZIP requires Java 17+."
  exit 1
fi
JAVA_VER=$(java -version 2>&1 | head -1)
info "Java found: $JAVA_VER"

# ── Step 2: Extract ZIP if needed ────────────────────────────────────────────
if [ ! -d "$KC_HOME" ]; then
  if [ ! -f "$KC_ZIP" ]; then
    error "Keycloak ZIP not found at $KC_ZIP"
    exit 1
  fi
  info "Extracting Keycloak 24 ZIP to $KC_EXTRACT_DIR ..."
  mkdir -p "$KC_EXTRACT_DIR"
  powershell.exe -Command "Expand-Archive -Path '$(cygpath -w "$KC_ZIP")' -DestinationPath '$(cygpath -w "$KC_EXTRACT_DIR")' -Force"
  info "Keycloak extracted to $KC_HOME"
else
  info "Keycloak already extracted at $KC_HOME"
fi

# ── Step 3: Copy theme into Keycloak themes directory ────────────────────────
THEME_SRC="$JIRA_DIR/keycloak-theme/jira-clone"
THEME_DEST="$KC_HOME/themes/jira-clone"
info "Copying JIRA Clone theme to Keycloak ..."
cp -rf "$THEME_SRC" "$THEME_DEST"
info "Theme copied to $THEME_DEST"

# ── Step 4: Kill any existing Keycloak on port 8180 ──────────────────────────
EXISTING=$(netstat -ano 2>/dev/null | grep ":${KC_PORT}" | grep LISTENING | awk '{print $5}' | head -1)
if [ -n "$EXISTING" ]; then
  warn "Port $KC_PORT in use (PID $EXISTING) — killing ..."
  taskkill //F //PID "$EXISTING" > /dev/null 2>&1 || true
  sleep 2
fi

# ── Step 5: Start Keycloak from ZIP ──────────────────────────────────────────
info "Starting Keycloak from ZIP (port $KC_PORT) ..."
KEYCLOAK_ADMIN=admin KEYCLOAK_ADMIN_PASSWORD=admin \
  "$KC_BIN" start-dev --http-port=$KC_PORT > "$LOG_DIR/keycloak.log" 2>&1 &
KC_PID=$!
info "Keycloak PID: $KC_PID"

# ── Step 6: Start DevSync MySQL (Docker) ─────────────────────────────────────
info "Starting DevSync MySQL via Docker ..."
if ! docker info > /dev/null 2>&1; then
  warn "Docker not running — skipping DevSync MySQL. Start it manually."
else
  cd "$PM_DIR"
  docker compose up -d mysql
  info "DevSync MySQL started"
fi

# ── Step 7: Wait for Keycloak ────────────────────────────────────────────────
wait_for_http "http://localhost:${KC_PORT}/realms/master" "Keycloak (ZIP)" 100

# ── Step 8: Configure realm ───────────────────────────────────────────────────
configure_keycloak_realm

# ── Step 9: Start JIRA Clone backend ─────────────────────────────────────────
info "Starting JIRA Clone backend (port 8080) ..."
cd "$JIRA_DIR/jira-clone-backend"
mvn spring-boot:run > "$LOG_DIR/jira-backend.log" 2>&1 &
info "JIRA Clone backend PID: $!"

# ── Step 10: Start DevSync backend ───────────────────────────────────────────
info "Starting DevSync backend (port 9090) ..."
cd "$PM_DIR/backend"
mvn spring-boot:run > "$LOG_DIR/devapp-backend.log" 2>&1 &
info "DevSync backend PID: $!"

# ── Step 11: Wait then start frontends ───────────────────────────────────────
warn "Waiting 30s for backends to initialise ..."
sleep 30

info "Starting JIRA Clone frontend (port 4200) ..."
cd "$JIRA_DIR/jira-clone-frontend"
npm start > "$LOG_DIR/jira-frontend.log" 2>&1 &
info "JIRA Clone frontend PID: $!"

info "Starting DevSync frontend (port 4300) ..."
cd "$PM_DIR/frontend"
npm start > "$LOG_DIR/devapp-frontend.log" 2>&1 &
info "DevSync frontend PID: $!"

# ── Done ─────────────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  All services starting (ZIP mode — no Docker KC)  ${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
echo ""
echo "  JIRA Clone UI       →  http://localhost:4200"
echo "  JIRA Clone API      →  http://localhost:8080/swagger-ui.html"
echo "  DevSync UI          →  http://localhost:4300"
echo "  DevSync API         →  http://localhost:9090/swagger-ui.html"
echo "  Keycloak Admin      →  http://localhost:8180  (admin / admin)"
echo "    Keycloak source: $KC_HOME"
echo "    realm: jira-clone | registration: ENABLED | theme: jira-clone"
echo ""
echo "  Logs:"
echo "    tail -f $LOG_DIR/keycloak.log"
echo "    tail -f $LOG_DIR/jira-backend.log"
echo "    tail -f $LOG_DIR/devapp-backend.log"
echo ""
