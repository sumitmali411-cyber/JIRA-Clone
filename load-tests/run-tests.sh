#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# run-tests.sh — Run smoke or load tests for both apps
#
# Usage:
#   bash load-tests/run-tests.sh smoke        # quick smoke test (always-available check)
#   bash load-tests/run-tests.sh load-jira    # load test JIRA Clone
#   bash load-tests/run-tests.sh load-devapp  # load test DevSync
#   bash load-tests/run-tests.sh load-all     # load test both
# ─────────────────────────────────────────────────────────────────────────────
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'

info()  { echo -e "${GREEN}[TEST]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[FAIL]${NC} $1"; }

# ── Install k6 if missing ────────────────────────────────────────────────────
if ! command -v k6 &> /dev/null; then
  warn "k6 not found — installing via winget ..."
  winget install k6 --silent
  export PATH="$PATH:/c/Program Files/k6"
fi

if ! command -v k6 &> /dev/null; then
  error "k6 still not found. Install manually: https://k6.io/docs/get-started/installation/"
  exit 1
fi

info "k6 $(k6 version)"

MODE=${1:-smoke}

case "$MODE" in
  smoke)
    info "Running smoke test (both apps) ..."
    k6 run "$SCRIPT_DIR/smoke.js"
    ;;
  load-jira)
    info "Running load test — JIRA Clone ..."
    k6 run "$SCRIPT_DIR/jira-clone-load.js"
    ;;
  load-devapp)
    info "Running load test — DevSync ..."
    k6 run "$SCRIPT_DIR/devapp-load.js"
    ;;
  load-all)
    info "Running load tests — JIRA Clone ..."
    k6 run "$SCRIPT_DIR/jira-clone-load.js"
    info "Running load tests — DevSync ..."
    k6 run "$SCRIPT_DIR/devapp-load.js"
    ;;
  *)
    echo "Usage: $0 [smoke|load-jira|load-devapp|load-all]"
    exit 1
    ;;
esac

info "Tests complete."
