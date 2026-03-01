#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# start.sh — Start ALL services with automatic Docker / ZIP fallback
#
# Decision logic:
#   Docker running? → Keycloak via Docker container  (port 8180)
#                   → DevSync MySQL via Docker        (port 3309)
#   Docker NOT running? → Keycloak from ZIP           (C:\comic\keycloak-24.0.0.zip)
#                       → DevSync MySQL must be local  (port 3309)
#
# Either way starts:
#   JIRA Clone backend  → http://localhost:8080  (auto-increments if busy)
#   DevSync backend     → http://localhost:9090
#   JIRA Clone UI       → http://localhost:4200
#   DevSync UI          → http://localhost:4300
# ─────────────────────────────────────────────────────────────────────────────
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JIRA_DIR="$SCRIPT_DIR"
PM_DIR="/c/Users/sumit/projects/Project-management-application"
LOG_DIR="/tmp/jira-logs"
mkdir -p "$LOG_DIR"

# Keycloak ZIP config
KC_ZIP="/c/comic/keycloak-24.0.0.zip"
KC_EXTRACT_DIR="/c/keycloak"
KC_HOME="$KC_EXTRACT_DIR/keycloak-24.0.0"
KC_PORT=8180

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; RED='\033[0;31m'; NC='\033[0m'
info()  { echo -e "${GREEN}[  OK ]${NC} $1"; }
warn()  { echo -e "${YELLOW}[ WAIT]${NC} $1"; }
mode()  { echo -e "${CYAN}[ MODE]${NC} $1"; }
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
  error "$label did not become ready — check logs in $LOG_DIR"
  return 1
}

configure_keycloak_realm() {
  info "Configuring Keycloak realm ..."
  TOKEN=$(curl -sf -X POST "http://localhost:${KC_PORT}/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=admin&password=admin&grant_type=password&client_id=admin-cli" \
    | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

  if [ -z "$TOKEN" ]; then
    warn "Could not get Keycloak admin token — configure realm manually at http://localhost:${KC_PORT}"
    return
  fi

  REALM_STATUS=$(curl -sf -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $TOKEN" \
    "http://localhost:${KC_PORT}/admin/realms/jira-clone" || echo "000")

  REALM_PAYLOAD='{
    "registrationAllowed": true,
    "registrationEmailAsUsername": false,
    "loginTheme": "jira-clone",
    "rememberMe": true,
    "resetPasswordAllowed": true
  }'

  if [ "$REALM_STATUS" = "200" ]; then
    curl -sf -X PUT "http://localhost:${KC_PORT}/admin/realms/jira-clone" \
      -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
      -d "$REALM_PAYLOAD"
    info "Realm 'jira-clone' updated — registration ON, theme: jira-clone"
  else
    curl -sf -X POST "http://localhost:${KC_PORT}/admin/realms" \
      -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
      -d '{
        "realm": "jira-clone", "enabled": true,
        "registrationAllowed": true, "registrationEmailAsUsername": false,
        "loginTheme": "jira-clone", "rememberMe": true, "resetPasswordAllowed": true,
        "clients": [{
          "clientId": "jira-clone-app", "enabled": true, "publicClient": true,
          "redirectUris": ["http://localhost:4200/*"], "webOrigins": ["http://localhost:4200"]
        }]
      }'
    info "Realm 'jira-clone' created — registration ON, theme: jira-clone"
  fi
}

start_keycloak_zip() {
  # ── Extract once ──────────────────────────────────────────────────────────
  if [ ! -d "$KC_HOME" ]; then
    if [ ! -f "$KC_ZIP" ]; then
      error "Keycloak ZIP not found at $KC_ZIP"
      exit 1
    fi
    info "Extracting Keycloak ZIP (one-time) ..."
    mkdir -p "$KC_EXTRACT_DIR"
    powershell.exe -Command "Expand-Archive -Path '$(cygpath -w "$KC_ZIP")' -DestinationPath '$(cygpath -w "$KC_EXTRACT_DIR")' -Force"
    info "Extracted to $KC_HOME"
  else
    info "Keycloak already extracted at $KC_HOME"
  fi

  # ── Copy theme (picks up any edits you make) ──────────────────────────────
  cp -rf "$JIRA_DIR/keycloak-theme/jira-clone" "$KC_HOME/themes/jira-clone"
  info "Theme synced to $KC_HOME/themes/jira-clone"

  # ── Kill anything on port 8180 ────────────────────────────────────────────
  EXISTING=$(netstat -ano 2>/dev/null | grep ":${KC_PORT}" | grep LISTENING | awk '{print $5}' | head -1)
  if [ -n "$EXISTING" ]; then
    warn "Port $KC_PORT in use (PID $EXISTING) — killing ..."
    taskkill //F //PID "$EXISTING" > /dev/null 2>&1 || true
    sleep 2
  fi

  # ── Start Keycloak ────────────────────────────────────────────────────────
  info "Starting Keycloak from ZIP on port $KC_PORT ..."
  KEYCLOAK_ADMIN=admin KEYCLOAK_ADMIN_PASSWORD=admin \
    "$KC_HOME/bin/kc.bat" start-dev --http-port=$KC_PORT \
    > "$LOG_DIR/keycloak.log" 2>&1 &
  info "Keycloak ZIP PID: $!"
}

start_keycloak_docker() {
  info "Starting Keycloak via Docker ..."
  cd "$JIRA_DIR"
  docker compose up -d
  info "Keycloak Docker container started"
}

start_mysql_docker() {
  info "Starting DevSync MySQL via Docker ..."
  cd "$PM_DIR"
  docker compose up -d mysql
  info "DevSync MySQL container started"
}

# ═══════════════════════════════════════════════════════════════════════════
# MAIN — detect Docker, choose mode
# ═══════════════════════════════════════════════════════════════════════════

echo ""
if docker info > /dev/null 2>&1; then
  mode "Docker is running → using Docker for Keycloak + MySQL"
  DOCKER_MODE=true
  start_keycloak_docker
  start_mysql_docker
else
  mode "Docker NOT running → falling back to ZIP Keycloak (C:\\comic\\keycloak-24.0.0.zip)"
  DOCKER_MODE=false
  if ! java -version > /dev/null 2>&1; then
    error "Java not found. ZIP Keycloak requires Java 17+."
    exit 1
  fi
  start_keycloak_zip
  warn "DevSync MySQL: Docker unavailable — ensure MySQL is running locally on port 3309"
fi

# ── Wait for Keycloak ─────────────────────────────────────────────────────
wait_for_http "http://localhost:${KC_PORT}/realms/master" "Keycloak" 100

# ── Configure realm ───────────────────────────────────────────────────────
configure_keycloak_realm

# ── Start both backends ───────────────────────────────────────────────────
info "Starting JIRA Clone backend ..."
cd "$JIRA_DIR/jira-clone-backend"
mvn spring-boot:run > "$LOG_DIR/jira-backend.log" 2>&1 &
info "JIRA Clone backend PID: $!"

info "Starting DevSync backend ..."
cd "$PM_DIR/backend"
mvn spring-boot:run > "$LOG_DIR/devapp-backend.log" 2>&1 &
info "DevSync backend PID: $!"

# ── Wait then start both frontends ────────────────────────────────────────
warn "Waiting 30s for backends to initialise ..."
sleep 30

info "Starting JIRA Clone frontend ..."
cd "$JIRA_DIR/jira-clone-frontend"
npm start > "$LOG_DIR/jira-frontend.log" 2>&1 &

info "Starting DevSync frontend ..."
cd "$PM_DIR/frontend"
npm start > "$LOG_DIR/devapp-frontend.log" 2>&1 &

# ── Summary ───────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}══════════════════════════════════════════════════════${NC}"
if [ "$DOCKER_MODE" = true ]; then
  echo -e "${GREEN}  Started in DOCKER mode                              ${NC}"
else
  echo -e "${CYAN}  Started in ZIP mode (no Docker)                     ${NC}"
fi
echo -e "${GREEN}══════════════════════════════════════════════════════${NC}"
echo ""
echo "  JIRA Clone UI       →  http://localhost:4200"
echo "  JIRA Clone API      →  http://localhost:8080/swagger-ui.html"
echo "  DevSync UI          →  http://localhost:4300"
echo "  DevSync API         →  http://localhost:9090/swagger-ui.html"
echo "  Keycloak Admin      →  http://localhost:${KC_PORT}   (admin / admin)"
echo "    realm: jira-clone | registration: ENABLED | theme: jira-clone"
echo ""
echo "  Logs:"
echo "    tail -f $LOG_DIR/jira-backend.log"
echo "    tail -f $LOG_DIR/devapp-backend.log"
echo "    tail -f $LOG_DIR/keycloak.log"
echo ""
