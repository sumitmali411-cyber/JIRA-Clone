/**
 * devapp-load.js — Load test for DevSync (Project Management) backend
 *
 * Auth: logs in via /api/v1/auth/login, reuses token across all VUs
 *
 * Run:
 *   k6 run load-tests/devapp-load.js
 *
 * Override:
 *   DEVAPP_API=http://localhost:9090 \
 *   DEVAPP_USER=admin@devapp.com DEVAPP_PASS=password \
 *   k6 run load-tests/devapp-load.js
 */
import http    from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

export const errorRate    = new Rate('errors');
export const projectsTime = new Trend('projects_list_duration');

export const options = {
  stages: [
    { duration: '30s', target: 10  },
    { duration: '2m',  target: 10  },
    { duration: '30s', target: 0   },
  ],
  thresholds: {
    errors:                ['rate<0.01'],
    http_req_duration:     ['p(95)<2000'],
    projects_list_duration:['p(95)<1500'],
  },
};

// ── Config ──────────────────────────────────────────────────────────────────
const DEVAPP_API  = __ENV.DEVAPP_API  || 'http://localhost:9090';
const DEVAPP_USER = __ENV.DEVAPP_USER || 'admin@devapp.com';
const DEVAPP_PASS = __ENV.DEVAPP_PASS || 'password';

// ── Auth ─────────────────────────────────────────────────────────────────────
export function setup() {
  const res = http.post(
    `${DEVAPP_API}/api/v1/auth/login`,
    JSON.stringify({ email: DEVAPP_USER, password: DEVAPP_PASS }),
    { headers: { 'Content-Type': 'application/json' } },
  );
  check(res, { 'login 200': (r) => r.status === 200 });
  if (res.status !== 200) {
    console.error(`Login failed: ${res.body}`);
    return { token: '' };
  }
  return { token: res.json('token') };
}

// ── Test ─────────────────────────────────────────────────────────────────────
export default function ({ token }) {
  const headers = token
    ? { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
    : { 'Content-Type': 'application/json' };

  group('Health', () => {
    const r = http.get(`${DEVAPP_API}/actuator/health`);
    const ok = check(r, { 'health 200': (r) => r.status === 200 });
    errorRate.add(!ok);
  });

  group('Projects', () => {
    const r = http.get(`${DEVAPP_API}/api/v1/projects`, { headers });
    projectsTime.add(r.timings.duration);
    const ok = check(r, { 'projects 200': (r) => r.status === 200 });
    errorRate.add(!ok);
  });

  group('Tasks', () => {
    const r = http.get(`${DEVAPP_API}/api/v1/tasks?projectId=1`, { headers });
    const ok = check(r, { 'tasks 200/404': (r) => [200, 404].includes(r.status) });
    errorRate.add(!ok);
  });

  group('Dashboard', () => {
    const r = http.get(`${DEVAPP_API}/api/v1/dashboard`, { headers });
    const ok = check(r, { 'dashboard 200': (r) => r.status === 200 });
    errorRate.add(!ok);
  });

  group('Swagger (public)', () => {
    const r = http.get(`${DEVAPP_API}/v3/api-docs`);
    const ok = check(r, { 'api-docs 200': (r) => r.status === 200 });
    errorRate.add(!ok);
  });

  sleep(1);
}
