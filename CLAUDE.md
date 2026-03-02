# CLAUDE.md — JIRA Clone

This file provides guidance to Claude Code when working in this repository.

---

## Project Overview

**JIRA Clone** — A full-stack project management tool with Kanban board, sprints, backlog, and issue tracking.
Authenticated via Keycloak SSO. Load-tested with k6, deployable to Docker or Kubernetes (minikube).

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Angular 20, PrimeNG 20.4.0 (Aura theme), standalone components |
| Backend | Java 17, Spring Boot 3.2.3, Maven |
| Auth | Keycloak 24 (OpenID Connect), Spring Security OAuth2 Resource Server |
| Database | MySQL 8 (`jiraclone` schema), Spring Data JPA (`ddl-auto=update`) |
| Infra | Docker Compose, Kubernetes (minikube), nginx |
| Load Tests | k6 |

---

## Quick Start

```bash
# Start everything (Docker-first, ZIP fallback)
bash start.sh

# Start only backend
cd jira-clone-backend
mvn spring-boot:run

# Start only frontend
cd jira-clone-frontend
npm start          # auto-selects port 4200-4210

# Health check (one-time)
bash health-check.sh --once

# Load test
bash load-tests/run-tests.sh smoke
```

---

## Development Commands

### Backend
```bash
cd jira-clone-backend
mvn compile                    # compile only
mvn spring-boot:run            # run (port 8080, fallback 8081-8090)
mvn clean install -DskipTests  # build JAR
mvn test                       # run tests
```

> **Windows note**: Use `mvn` not `./mvnw`. If `mvn` not in PATH, use full path or fix PATH.

### Frontend
```bash
cd jira-clone-frontend
npm install --legacy-peer-deps  # first time only (ngx-quill peer dep conflict)
npm start                       # ng serve, auto port 4200-4210
npm run build                   # production build to dist/
npm run lint                    # ESLint
```

### Keycloak (Docker)
```bash
docker-compose up -d keycloak mysql   # start only KC + MySQL
docker-compose logs -f keycloak       # tail logs
# Admin console: http://localhost:8180 (admin/admin)
```

---

## Key URLs (Local Dev)

| Service | URL |
|---------|-----|
| Frontend | http://localhost:4200 |
| Backend Swagger | http://localhost:8080/swagger-ui.html |
| Backend API Docs | http://localhost:8080/v3/api-docs |
| Keycloak Admin | http://localhost:8180/admin |
| Keycloak Realm | http://localhost:8180/realms/jira-clone |
| GitHub Webhook (returns 401 unauthenticated) | http://localhost:8080/api/github/webhook |

---

## Key File Paths

| What | Where |
|------|-------|
| Backend config | `jira-clone-backend/src/main/resources/application.properties` |
| Security config | `jira-clone-backend/src/main/java/com/jiraclone/config/SecurityConfig.java` |
| Port fallback | `jira-clone-backend/src/main/java/com/jiraclone/config/PortCustomizer.java` |
| GitHub webhook | `jira-clone-backend/src/main/java/com/jiraclone/controller/GitHubWebhookController.java` |
| Keycloak service | `jira-clone-frontend/src/app/core/services/keycloak.service.ts` |
| Auth interceptor | `jira-clone-frontend/src/app/core/interceptors/auth.interceptor.ts` |
| App config | `jira-clone-frontend/src/app/app.config.ts` |
| Environment dev | `jira-clone-frontend/src/environments/environment.ts` |
| Environment prod | `jira-clone-frontend/src/environments/environment.prod.ts` |
| K8s manifests | `k8s/` (00-namespace → 07-ingress) |
| Docker Compose | `docker-compose.yml` (root) |
| Keycloak theme | `keycloak-theme/jira-clone/login/` |
| Load tests | `load-tests/` (smoke.js, jira-clone-load.js) |
| Health monitor | `health-check.sh` |
| Unified start | `start.sh` |

---

## Database

- **DB name**: `jiraclone`
- **User**: `root` / **Password**: `root`
- **JPA**: `ddl-auto=update` — schema auto-updates on startup
- Collections stored as JSON TEXT via `AttributeConverter`
  - `StringListConverter` for `List<String>` labels/assignees
  - `IssueLinkListConverter` for `List<IssueLink>` (POJO, not entity)
  - `GitCommitListConverter` for `List<GitCommit>` on Issue

---

## Authentication (Keycloak)

- **Realm**: `jira-clone` | **Client**: `jira-clone-app` (public SPA)
- **Redirect URIs**: `http://localhost:4200/*`
- **Registration**: enabled (`registrationAllowed: true`)
- **Custom theme**: `jira-clone` (FTL template + custom CSS at `keycloak-theme/`)
- Backend validates JWTs via `spring.security.oauth2.resourceserver.jwt.issuer-uri`
- Frontend `KeycloakService.init()` must be wrapped in try/catch (fallback: app loads without auth)

---

## Kubernetes (minikube)

```bash
# Start cluster
minikube start --driver=docker --cpus=4 --memory=6g
minikube addons enable ingress

# Build images inside minikube
eval $(minikube docker-env)
docker build -t jira-clone-backend:1.0.0 jira-clone-backend/
docker build -t jira-clone-frontend:1.0.0 jira-clone-frontend/

# Apply manifests
kubectl apply -f k8s/

# Expose via tunnel (run as admin in separate terminal)
minikube tunnel

# Add hosts entries (as admin)
# 127.0.0.1  jira-clone.local
# 127.0.0.1  auth.jira-clone.local
```

**K8s manifests order**: `00-namespace` → `01-secrets` → `02-configmap` → `03-mysql` → `04-keycloak` → `05-backend` → `06-frontend` → `07-ingress`

---

## Load Testing

```bash
bash load-tests/run-tests.sh smoke        # quick sanity (all services)
bash load-tests/run-tests.sh load-jira    # JIRA Clone load test (k6)
bash load-tests/run-tests.sh load-all     # both apps
INTERVAL=60 bash health-check.sh          # continuous monitoring every 60s
```

---

## Best Practices

### Spring Boot
- Constructor injection only — never `@Autowired` on fields
- `@Transactional` on all write service methods; `@Transactional(readOnly=true)` on reads
- DTOs for all API responses — never expose JPA entities directly
- `permitAll()` only for: Swagger, api-docs, actuator/health, Keycloak JWKS
- Add `server.shutdown=graceful` for zero-downtime K8s rolling updates
- Never log passwords, tokens, or PII

### Angular
- Standalone components only (no NgModules)
- `async` pipe in templates — never manually subscribe without `takeUntilDestroyed()`
- HTTP calls only in services
- `KeycloakService.init()` must have try/catch; `APP_INITIALIZER` must have `.catch(() => {})`
- `auth.interceptor.ts` must have `catchError` so failed token fetch doesn't crash requests
- `environment.prod.ts` must have the same Keycloak config block as dev

### Docker / K8s
- Never use `imagePullPolicy: Always` with local minikube builds — use `Never`
- K8s: `command:` overrides Docker ENTRYPOINT; use `args:` to pass subcommands (critical for Keycloak)
- Secrets in `k8s/01-secrets.yaml` (gitignored); commit only `.example` template
- HPA: CPU target 70%, memory 80%, min 2 replicas
- Always define `livenessProbe` + `readinessProbe`

### Keycloak
- In K8s: use `args:` not `command:` so ENTRYPOINT (`kc.sh`) is preserved
- Create `keycloak` database in MySQL before Keycloak starts
- Theme: mount `keycloak-theme/jira-clone` as volume to `/opt/keycloak/themes/jira-clone`
- Fallback: if Docker unavailable, ZIP at `c:/comic/keycloak-24.0.0.zip` extracted to `c:/comic/keycloak-24.0.0/`

### Security
- `k8s/01-secrets.yaml` is gitignored — never commit real secrets
- Run `git log --all -p | grep -i password` to check for accidental secret commits
- Rotate Keycloak admin password after first production deployment
- CORS: read origins from `@Value("${app.cors.allowed-origins}")` — not hardcoded
- Webhook: validate `X-Hub-Signature-256` HMAC-SHA256

---

## Available Slash Commands (this project)

| Command | Purpose |
|---------|---------|
| `/start-jira` | Start JIRA Clone backend + frontend |
| `/build-jira` | Build backend JAR + frontend dist |
| `/health-jira` | Run health check for JIRA services |

## Available Global Slash Commands

| Command | Purpose |
|---------|---------|
| `/architect` | Full architectural review |
| `/review` | Code review with OWASP + Spring + Angular checks |
| `/angular-check` | Angular best practices audit |
| `/spring-check` | Spring Boot best practices audit |
| `/docker-check` | Docker/K8s configuration audit |
| `/keycloak-check` | Keycloak setup verification |
| `/security-audit` | OWASP Top 10 security audit |
| `/server-check` | Check which services are running and healthy |
