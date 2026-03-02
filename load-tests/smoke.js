/**
 * smoke.js — Always-available smoke test for BOTH apps
 * Runs fast (< 30s), zero load, just verifies every critical endpoint responds.
 *
 * Run: k6 run load-tests/smoke.js
 */
import http from 'k6/http';
import { check, group } from 'k6';
import { Rate } from 'k6/metrics';

export const errorRate = new Rate('errors');

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    errors:            ['rate==0'],          // zero errors allowed in smoke
    http_req_duration: ['p(95)<3000'],
  },
};

// ── Config ────────────────────────────────────────────────────────────────────
const JIRA_API   = __ENV.JIRA_API   || 'http://localhost:8080';
const DEVAPP_API = __ENV.DEVAPP_API || 'http://localhost:9090';
const KEYCLOAK   = __ENV.KEYCLOAK   || 'http://localhost:8180';

// ── Helpers ───────────────────────────────────────────────────────────────────
function ok(tag, res, expectedStatus = 200) {
  const passed = check(res, {
    [`${tag} status ${expectedStatus}`]: (r) => r.status === expectedStatus,
  });
  errorRate.add(!passed);
  if (!passed) console.error(`FAIL [${tag}] → HTTP ${res.status} (expected ${expectedStatus})`);
}

// ── Tests ─────────────────────────────────────────────────────────────────────
export default function () {

  group('Keycloak', () => {
    ok('KC realm reachable', http.get(`${KEYCLOAK}/realms/master`));
    ok('KC jira-clone realm', http.get(`${KEYCLOAK}/realms/jira-clone`));
  });

  group('JIRA Clone — public endpoints', () => {
    ok('swagger-ui',   http.get(`${JIRA_API}/swagger-ui.html`));
    ok('api-docs',     http.get(`${JIRA_API}/v3/api-docs`));
    ok('webhook POST returns 401 (security active)',
      http.post(`${JIRA_API}/api/github/webhook`, '{}', { headers: { 'Content-Type': 'application/json' } }),
      401);
  });

  group('DevSync — public endpoints', () => {
    ok('swagger-ui',  http.get(`${DEVAPP_API}/swagger-ui.html`));
    ok('api-docs',    http.get(`${DEVAPP_API}/v3/api-docs`));
    ok('health',      http.get(`${DEVAPP_API}/actuator/health`));
    ok('auth endpoint reachable (405 = wrong method = endpoint exists)',
      http.get(`${DEVAPP_API}/api/v1/auth/login`),
      405);
  });
}
