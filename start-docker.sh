#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# start-docker.sh  — Start ALL services using Docker for Keycloak
#
# What this starts:
#   [Docker]  Keycloak 24        → http://localhost:8180
#   [Docker]  DevSync MySQL      → localhost:3309
#   [Local]   JIRA Clone backend → http://localhost:8080  (requires MySQL at :3306)
#   [Local]   DevSync backend    → http://localhost:9090
#   [Local]   JIRA Clone UI      → http://localhost:4200
#   [Local]   DevSync UI         → http://localhost:4300
#
# Prerequisites:
#   - Docker Desktop running
#   - MySQL running locally at port 3306 (for JIRA Clone)
#   - Java 17+, Maven, Node.js installed
# ─────────────────────────────────────────────────────────────────────────────
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JIRA_DIR="$SCRIPT_DIR"
PM_DIR="/c/Users/sumit/projects/Project-management-application"
LOG_DIR="/tmp/jira-logs"
mkdir -p "$LOG_DIR"

GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${GREEN}[START]${NC} $1"; }
warn()    { echo -e "${YELLOW}[ WAIT]${NC} $1"; }
error()   { echo -e "${RED}[ERROR]${NC} $1"; }

wait_for_http() {
  local url=$1 label=$2 max=${3:-60}
  warn "Waiting for $label at $url ..."
  for i in $(seq 1 $max); do
    if curl -sf "$url" > /dev/null 2>&1; then
      info "$label is ready"
      return 0
    fi
    sleep 3
  done
  error "$label did not start in time"
  return 1
}

configure_keycloak_realm() {
  info "Configuring Keycloak realm (registration + theme) ..."

  # Get admin token
  TOKEN=$(curl -sf -X POST "http://localhost:8180/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "username=admin&password=admin&grant_type=password&client_id=admin-cli" \
    | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)

  if [ -z "$TOKEN" ]; then
    warn "Could not get admin token — configure realm manually at http://localhost:8180"
    return
  fi

  # Check if jira-clone realm exists
  REALM_EXISTS=$(curl -sf -o /dev/null -w "%{http_code}" \
    -H "Authorization: Bearer $TOKEN" \
    "http://localhost:8180/admin/realms/jira-clone" || echo "000")

  if [ "$REALM_EXISTS" = "200" ]; then
    # Update existing realm: enable registration + set theme
    curl -sf -X PUT "http://localhost:8180/admin/realms/jira-clone" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "registrationAllowed": true,
        "registrationEmailAsUsername": false,
        "loginTheme": "jira-clone",
        "rememberMe": true,
        "resetPasswordAllowed": true
      }' && info "Realm 'jira-clone' updated: registration ENABLED, theme set to 'jira-clone'"
  else
    # Create realm from scratch
    curl -sf -X POST "http://localhost:8180/admin/realms" \
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

# ── Step 1: Docker check ──────────────────────────────────────────────────────
info "Checking Docker ..."
if ! docker info > /dev/null 2>&1; then
  error "Docker is not running. Please start Docker Desktop and retry."
  exit 1
fi
info "Docker is running"

# ── Step 2: Start Keycloak (Docker) ──────────────────────────────────────────
info "Starting Keycloak via Docker ..."
cd "$JIRA_DIR"
docker compose up -d
info "Keycloak container started"

# ── Step 3: Start DevSync MySQL (Docker) ─────────────────────────────────────
info "Starting DevSync MySQL via Docker ..."
cd "$PM_DIR"
docker compose up -d mysql
info "DevSync MySQL container started"

# ── Step 4: Wait for services ─────────────────────────────────────────────────
wait_for_http "http://localhost:8180/realms/master" "Keycloak" 80
wait_for_http "http://localhost:3309" "DevSync MySQL" 40 2>/dev/null || \
  warn "MySQL port check skipped (TCP-only) — should be ready"

# ── Step 5: Auto-configure Keycloak realm ─────────────────────────────────────
configure_keycloak_realm

# ── Step 6: Start JIRA Clone backend ─────────────────────────────────────────
info "Starting JIRA Clone backend (port 8080) ..."
cd "$JIRA_DIR/jira-clone-backend"
mvn spring-boot:run > "$LOG_DIR/jira-backend.log" 2>&1 &
JIRA_BACKEND_PID=$!
info "JIRA Clone backend PID: $JIRA_BACKEND_PID"

# ── Step 7: Start DevSync backend ────────────────────────────────────────────
info "Starting DevSync backend (port 9090) ..."
cd "$PM_DIR/backend"
mvn spring-boot:run > "$LOG_DIR/devapp-backend.log" 2>&1 &
PM_BACKEND_PID=$!
info "DevSync backend PID: $PM_BACKEND_PID"

# ── Step 8: Wait for backends then start frontends ───────────────────────────
warn "Waiting 30s for backends to start ..."
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
echo -e "${GREEN}  All services starting — check logs in /tmp/jira-logs/${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════${NC}"
echo ""
echo "  JIRA Clone UI       →  http://localhost:4200"
echo "  JIRA Clone API      →  http://localhost:8080/swagger-ui.html"
echo "  DevSync UI          →  http://localhost:4300"
echo "  DevSync API         →  http://localhost:9090/swagger-ui.html"
echo "  Keycloak Admin      →  http://localhost:8180  (admin / admin)"
echo "    realm: jira-clone | registration: ENABLED | theme: jira-clone"
echo ""
echo "  Logs:"
echo "    tail -f $LOG_DIR/jira-backend.log"
echo "    tail -f $LOG_DIR/devapp-backend.log"
echo "    tail -f $LOG_DIR/jira-frontend.log"
echo ""
