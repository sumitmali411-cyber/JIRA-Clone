/**
 * jira-clone-load.js — Load test for JIRA Clone backend
 *
 * Stages:  ramp-up 30s → steady 2min → ramp-down 30s
 * Auth:    Fetches Keycloak token once per VU, reuses it for all requests
 *
 * Run:
 *   k6 run load-tests/jira-clone-load.js
 *
 * Override defaults:
 *   JIRA_API=http://localhost:8080 KEYCLOAK=http://localhost:8180 \
 *   KC_USER=testuser KC_PASS=password k6 run load-tests/jira-clone-load.js
 */
import http    from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

export const errorRate    = new Rate('errors');
export const projectsTime = new Trend('projects_list_duration');

export const options = {
  stages: [
    { duration: '30s', target: 10  },   // ramp up
    { duration: '2m',  target: 10  },   // steady load
    { duration: '30s', target: 0   },   // ramp down
  ],
  thresholds: {
    errors:                ['rate<0.01'],    // < 1% errors
    http_req_duration:     ['p(95)<2000'],   // 95th percentile < 2s
    projects_list_duration:['p(95)<1500'],
  },
};

// ── Config ─────────────────────────────────────────────────────────────────
const JIRA_API  = __ENV.JIRA_API  || 'http://localhost:8080';
const KEYCLOAK  = __ENV.KEYCLOAK  || 'http://localhost:8180';
const KC_REALM  = __ENV.KC_REALM  || 'jira-clone';
const KC_CLIENT = __ENV.KC_CLIENT || 'jira-clone-app';
const KC_USER   = __ENV.KC_USER   || 'testuser';
const KC_PASS   = __ENV.KC_PASS   || 'password';

// ── Auth (once per VU) ─────────────────────────────────────────────────────
let token = '';

export function setup() {
  // Fetch token once (shared across all VUs via return value)
  const res = http.post(
    `${KEYCLOAK}/realms/${KC_REALM}/protocol/openid-connect/token`,
    { username: KC_USER, password: KC_PASS, grant_type: 'password', client_id: KC_CLIENT },
  );
  check(res, { 'token obtained': (r) => r.status === 200 });
  if (res.status !== 200) {
    console.error(`Could not get Keycloak token: ${res.body}`);
    return { token: '' };
  }
  return { token: res.json('access_token') };
}

// ── Test ────────────────────────────────────────────────────────────────────
export default function ({ token }) {
  const headers = token
    ? { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
    : { 'Content-Type': 'application/json' };

  group('Projects', () => {
    const r = http.get(`${JIRA_API}/api/projects`, { headers });
    projectsTime.add(r.timings.duration);
    const ok = check(r, { 'list projects 200': (r) => r.status === 200 });
    errorRate.add(!ok);
  });

  group('Issues', () => {
    // List issues — replace projectId with one that exists in your DB
    const r = http.get(`${JIRA_API}/api/issues?projectId=1`, { headers });
    const ok = check(r, { 'list issues 200/404': (r) => [200, 404].includes(r.status) });
    errorRate.add(!ok);
  });

  group('Swagger (public)', () => {
    const r = http.get(`${JIRA_API}/v3/api-docs`);
    const ok = check(r, { 'api-docs 200': (r) => r.status === 200 });
    errorRate.add(!ok);
  });

  sleep(1);
}
