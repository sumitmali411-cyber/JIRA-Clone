#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# health-check.sh — Continuously monitors all services, alerts on failure
#
# Usage:
#   bash health-check.sh            # runs forever, checks every 30s
#   bash health-check.sh --once     # single check then exit (for CI)
#   INTERVAL=60 bash health-check.sh
# ─────────────────────────────────────────────────────────────────────────────

INTERVAL=${INTERVAL:-30}
ONCE=false
[[ "${1}" == "--once" ]] && ONCE=true

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'
PASS=0; FAIL=0

check() {
  local label=$1 url=$2 expected=${3:-200}
  local code
  code=$(curl -sf -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null || echo "000")
  if [ "$code" = "$expected" ]; then
    echo -e "  ${GREEN}✓${NC} $label ($code)"
    ((PASS++)) || true
  else
    echo -e "  ${RED}✗${NC} $label — expected $expected, got $code  ← $url"
    ((FAIL++)) || true
  fi
}

run_checks() {
  PASS=0; FAIL=0
  local ts
  ts=$(date '+%H:%M:%S')
  echo ""
  echo -e "${YELLOW}[$ts] Health check ────────────────────────────────${NC}"

  echo "  Keycloak"
  check "  master realm"        "http://localhost:8180/realms/master"
  check "  jira-clone realm"    "http://localhost:8180/realms/jira-clone"

  echo "  JIRA Clone"
  check "  swagger UI"          "http://localhost:8080/swagger-ui.html"
  check "  api-docs"            "http://localhost:8080/v3/api-docs"
  check "  frontend"            "http://localhost:4200"
  check "  webhook (auth=401)"  "http://localhost:8080/api/github/webhook" "401"

  echo "  DevSync"
  check "  health"              "http://localhost:9090/actuator/health"
  check "  swagger UI"          "http://localhost:9090/swagger-ui.html"
  check "  api-docs"            "http://localhost:9090/v3/api-docs"
  check "  frontend"            "http://localhost:4300"

  # Summary
  local total=$((PASS + FAIL))
  if [ "$FAIL" -eq 0 ]; then
    echo -e "  ${GREEN}All $total checks passed${NC}"
  else
    echo -e "  ${RED}$FAIL/$total checks FAILED${NC}"
  fi
}

if $ONCE; then
  run_checks
  exit $FAIL
fi

# ── Continuous loop ───────────────────────────────────────────────────────────
echo "Monitoring all services every ${INTERVAL}s  (Ctrl+C to stop)"
while true; do
  run_checks
  sleep "$INTERVAL"
done
