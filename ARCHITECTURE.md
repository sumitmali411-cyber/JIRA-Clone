# JIRA Clone — Complete Architecture & Usage Documentation

> Generated: 2026-03-01
> Stack: Angular 20 + Spring Boot 3.2.3 + MySQL + Keycloak 24 (Docker)

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Quick Start](#3-quick-start)
4. [Port Map](#4-port-map)
5. [Docker & Keycloak Setup](#5-docker--keycloak-setup)
6. [Keycloak User Management](#6-keycloak-user-management)
7. [System Architecture](#7-system-architecture)
8. [Authentication Flow](#8-authentication-flow)
9. [Fallback Behaviour (Keycloak Down)](#9-fallback-behaviour-keycloak-down)
10. [Backend Reference](#10-backend-reference)
11. [Frontend Reference](#11-frontend-reference)
12. [API Endpoints](#12-api-endpoints)
13. [Environment Configuration](#13-environment-configuration)
14. [Common Commands Cheat Sheet](#14-common-commands-cheat-sheet)
15. [Startup Order](#15-startup-order)
16. [Troubleshooting](#16-troubleshooting)

---

## 1. Project Overview

JIRA Clone is a full-stack project management tool replicating core JIRA features:

| Feature | Route | Description |
|---------|-------|-------------|
| Projects | `/projects` | Create and manage projects |
| Kanban Board | `/projects/:id/board` | Drag-and-drop issue management by status |
| Backlog & Sprints | `/projects/:id/backlog` | Sprint planning, backlog grooming |
| Issue Detail | `/issues/:id` | Rich editing, comments, activity log |
| Authentication | — | Keycloak-backed SSO via OAuth2/OIDC |

---

## 2. Technology Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| Frontend framework | Angular | 20.1.0 | SPA |
| UI component library | PrimeNG | 20.4.0 | Components + Aura theme |
| UI theme | @primeuix/themes (Aura) | 2.0.3 | Design system |
| Rich-text editor | ngx-quill | 30.0.1 | Issue description |
| Charts | Chart.js | 4.5.1 | Analytics / reports |
| Auth client (browser) | keycloak-js | 26.2.3 | OIDC login flow |
| Material components | @angular/material | 20.2.14 | Drag-and-drop, overlays |
| Backend framework | Spring Boot | 3.2.3 | REST API |
| Runtime | Java (OpenJDK) | 17 | JVM |
| ORM | Spring Data JPA (Hibernate) | Boot managed | Database layer |
| Security | spring-security + oauth2-resource-server | Boot managed | JWT validation |
| Database | MySQL | 8.x | Persistent storage |
| Identity provider | Keycloak | 24.0.1 | SSO / OAuth2 / OIDC |
| Container runtime | Docker Desktop | latest | Runs Keycloak only |
| Excel export | Apache POI (poi-ooxml) | 5.2.5 | Issue export |
| HTML parsing | jsoup | 1.17.2 | Export cleanup |
| Boilerplate reduction | Lombok | Boot managed | `@Data`, `@Builder`, etc. |

---

## 3. Quick Start

### Prerequisites

- Java 17 or later
- Maven 3.8 or later (use `mvn`, not `./mvnw` on Windows)
- Node.js 18 or later + npm
- MySQL 8 running locally — user `root`, password `sam9311`
- Docker Desktop running

### Steps

```bash
# Step 1 — Start Keycloak via Docker
cd C:\Users\sumit\projects\JIRA-Clone
docker compose up -d

# Step 2 — Start Spring Boot backend
cd jira-clone-backend
mvn spring-boot:run

# Step 3 — Start Angular frontend
cd jira-clone-frontend
npm start
```

Open http://localhost:4200 — you will be redirected to Keycloak login.
Login with `testuser` / `testuser` (or any user you created in the realm).

---

## 4. Port Map

| Service | URL | Notes |
|---------|-----|-------|
| Angular Frontend | http://localhost:4200 | `npm start` |
| Spring Boot API | http://localhost:8080/api | `mvn spring-boot:run` |
| Keycloak Admin | http://localhost:8180 | Docker — admin / admin |
| Keycloak realm endpoint | http://localhost:8180/realms/jira-clone | Used by Spring Boot for JWT validation |
| MySQL | localhost:3306 | Native install — root / sam9311 |

---

## 5. Docker & Keycloak Setup

### What Docker runs

In this project **Docker runs only Keycloak**. MySQL, Spring Boot, and Angular all run natively on your machine. Docker Desktop must be running before you start Keycloak.

### docker-compose.yml — line-by-line

```yaml
services:
  keycloak:
    image: quay.io/keycloak/keycloak:24.0.1   # Official image, pinned to 24.0.1
    command: start-dev                          # Dev mode: HTTP OK, no TLS required
    environment:
      KEYCLOAK_ADMIN: admin                     # Admin console username
      KEYCLOAK_ADMIN_PASSWORD: admin            # Admin console password
    ports:
      - "8180:8080"                             # Map host :8180 → container :8080
                                                # (Spring Boot already owns :8080)
```

> **Why 8180?** Keycloak's internal port is 8080. Spring Boot also uses 8080. Mapping to 8180 avoids the clash.

### Docker commands

```bash
# Start Keycloak in background
docker compose up -d

# Stop Keycloak (keeps data)
docker compose down

# Stop AND wipe all realm data (full reset)
docker compose down -v

# View running containers
docker ps

# Status + health of compose services
docker compose ps

# Live log stream
docker compose logs -f keycloak

# Restart without stopping
docker compose restart keycloak

# Check if Keycloak is responding
curl http://localhost:8180/realms/jira-clone
# Returns JSON if up; connection refused if down
```

### Verify Keycloak is ready

After `docker compose up -d`, wait ~10 seconds, then:

```bash
curl http://localhost:8180/realms/jira-clone
```

You should see a JSON object starting with `{"realm":"jira-clone",...}`.
If you get "connection refused", Keycloak is still starting — wait and retry.

---

## 6. Keycloak User Management

### Access the Admin Console

1. Open http://localhost:8180
2. Click **Administration Console**
3. Login: `admin` / `admin`
4. Top-left dropdown → select realm **jira-clone**

---

### View All Users

1. Left sidebar → **Users**
2. Table lists every user in the realm
3. Click any username to open the detail view

---

### Create a New User

1. **Users** → **Create new user**
2. Fill in the form:

   | Field | Example |
   |-------|---------|
   | Username | `alice` |
   | Email | `alice@example.com` |
   | First name | `Alice` |
   | Last name | `Smith` |
   | Email verified | ON |

3. Click **Create**
4. Open the **Credentials** tab
5. Click **Set password** → enter password → set **Temporary** to **OFF**
6. Click **Save password**

The user can now log in at http://localhost:4200.

---

### Edit a User

1. **Users** → click the username
2. Edit fields on the **Details** tab
3. Click **Save**

---

### Reset a User's Password

1. **Users** → click user → **Credentials** tab
2. Click **Reset password**
3. Enter new password, set **Temporary** OFF if you don't want forced reset
4. Click **Save password**

---

### Delete a User

1. **Users** → click user
2. Scroll to the bottom → **Delete**
3. Confirm the deletion dialog

---

### Assign Roles

1. **Users** → click user → **Role mapping** tab
2. Click **Assign role**
3. Search, select role → click **Assign**

---

### View Active Sessions

1. **Users** → click user → **Sessions** tab
2. Shows all active login sessions
3. You can force-logout individual sessions from here

---

### Test Login in Incognito

1. Open a private/incognito browser window
2. Go to http://localhost:4200
3. You are redirected to Keycloak login — enter your user's credentials
4. After login you are redirected back to the Angular app

---

### Realm & Client Configuration Reference

**Realm: `jira-clone`**

| Setting | Value |
|---------|-------|
| Realm name | `jira-clone` |
| Enabled | Yes |

**Client: `jira-clone-app`**

| Setting | Value |
|---------|-------|
| Client ID | `jira-clone-app` |
| Client type | OpenID Connect |
| Client authentication | OFF (public client) |
| Standard Flow | Enabled |
| Valid Redirect URIs | `http://localhost:4200/*` |
| Web Origins | `http://localhost:4200` |

---

### Create the Realm from Scratch (if missing)

1. Admin Console → top-left dropdown → **Create Realm**
2. Realm name: `jira-clone` → **Enabled ON** → **Create**
3. Left sidebar → **Clients** → **Create client**
   - Client ID: `jira-clone-app`
   - Type: OpenID Connect → **Next**
   - Standard Flow: ON → **Next**
   - Root URL: `http://localhost:4200`
   - Valid Redirect URIs: `http://localhost:4200/*`
   - Web Origins: `http://localhost:4200`
   - Click **Save**
4. Create at least one test user (see [Create a New User](#create-a-new-user))

---

### Keycloak REST API — Manage Users Programmatically

```bash
# 1. Get an admin access token
TOKEN=$(curl -s -X POST http://localhost:8180/realms/master/protocol/openid-connect/token \
  -d "client_id=admin-cli&username=admin&password=admin&grant_type=password" \
  | jq -r '.access_token')

# 2. List users in jira-clone realm
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8180/admin/realms/jira-clone/users

# 3. Create a user via API
curl -X POST http://localhost:8180/admin/realms/jira-clone/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "email": "new@example.com",
    "firstName": "New",
    "lastName": "User",
    "enabled": true,
    "emailVerified": true,
    "credentials": [{"type":"password","value":"password123","temporary":false}]
  }'

# 4. Search for a user by username
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8180/admin/realms/jira-clone/users?username=alice"

# 5. Delete a user (get the ID from the list first)
curl -X DELETE -H "Authorization: Bearer $TOKEN" \
  http://localhost:8180/admin/realms/jira-clone/users/<USER_ID>
```

---

## 7. System Architecture

```
┌───────────────────────────────────────────────────────────────────┐
│            Browser — Angular 20  (http://localhost:4200)          │
│  ┌──────────────┐  ┌────────┐  ┌──────────┐  ┌────────────────┐  │
│  │  /projects   │  │ /board │  │ /backlog │  │  /issues/:id   │  │
│  └──────────────┘  └────────┘  └──────────┘  └────────────────┘  │
│  auth.interceptor → attaches Bearer JWT to every HTTP request     │
└────────────────┬────────────────────────────┬─────────────────────┘
                 │                            │
           REST + JWT                    OIDC login
                 │                            │
    ┌────────────▼────────────┐  ┌────────────▼────────────────────┐
    │  Spring Boot 3.2.3      │  │  Keycloak 24 (Docker)           │
    │  http://localhost:8080  │◄─┤  http://localhost:8180          │
    │                         │  │                                  │
    │  REST Controllers (8)   │  │  Realm: jira-clone              │
    │  Service Layer (8)      │  │  Client: jira-clone-app         │
    │  JPA Repositories (6)   │  │  Admin: admin / admin           │
    │  Spring Security / JWT  │  │  Users stored in realm          │
    │  CORS + Exception Handler│  │  JWT tokens issued here         │
    │  Excel Export / Search  │  │  port 8180:8080 (Docker)        │
    └────────────┬────────────┘  └─────────────────────────────────┘
                 │
           JPA / JDBC
                 │
    ┌────────────▼────────────┐
    │  MySQL  (localhost:3306)│
    │  DB: jiraclone          │
    │  user: root             │
    │  DDL: auto-update       │
    │  Tables: projects,      │
    │  issues, sprints,       │
    │  comments,              │
    │  activity_logs,         │
    │  app_config             │
    └─────────────────────────┘
```

### Backend Package Structure

```
com.jiraclone/
├── JiraCloneApplication.java           Entry point (@SpringBootApplication)
├── config/
│   ├── CorsConfig.java                 Allowed origins / methods / headers
│   ├── GlobalExceptionHandler.java     @ControllerAdvice — unified error shape
│   └── SecurityConfig.java            JWT resource server, CSRF off, CORS on
├── controller/                         REST layer (all require JWT)
│   ├── ActivityController.java
│   ├── CommentController.java
│   ├── ConfigController.java
│   ├── ExportController.java
│   ├── IssueController.java
│   ├── ProjectController.java
│   ├── SearchController.java
│   └── SprintController.java
├── converter/                          JPA AttributeConverters for JSON columns
│   ├── IssueLinkListConverter.java     List<IssueLink> ↔ TEXT (JSON)
│   └── StringListConverter.java       List<String> ↔ TEXT (JSON)
├── dto/                                Request / Response bodies (9 DTOs)
│   ├── ApiResponse.java
│   ├── CommentDto.java
│   ├── IssueDto.java
│   ├── IssueLinkDto.java
│   ├── ProjectDto.java
│   ├── SearchFilterDto.java
│   ├── SprintAssignDto.java
│   ├── SprintDto.java
│   └── StatusUpdateDto.java
├── model/                              JPA entities
│   ├── ActivityLog.java
│   ├── AppConfig.java
│   ├── Comment.java
│   ├── Issue.java
│   ├── IssueLink.java                  POJO (not an entity; stored inside Issue.links as JSON)
│   ├── Project.java
│   ├── Sprint.java
│   └── enums/
│       ├── IssueType.java
│       ├── LinkType.java
│       ├── Priority.java
│       ├── ProjectType.java
│       └── SprintStatus.java
├── repository/                         Spring Data JPA interfaces
│   ├── ActivityLogRepository.java
│   ├── AppConfigRepository.java
│   ├── CommentRepository.java
│   ├── IssueRepository.java
│   ├── ProjectRepository.java
│   └── SprintRepository.java
└── service/                            Business logic
    ├── ActivityService.java
    ├── CommentService.java
    ├── ConfigService.java
    ├── ExportService.java
    ├── IssueService.java
    ├── ProjectService.java
    ├── SearchService.java
    └── SprintService.java
```

### Frontend Module Structure

```
src/app/
├── app.config.ts         Providers: Keycloak init, PrimeNG, Router, HTTP interceptors
├── app.routes.ts         Lazy-loaded route definitions
├── app.ts                Root component
├── core/
│   ├── interceptors/
│   │   └── auth.interceptor.ts   Adds Bearer JWT to every HTTP request (functional interceptor)
│   ├── models/                   TypeScript interfaces
│   │   ├── activity.model.ts
│   │   ├── api-response.model.ts
│   │   ├── comment.model.ts
│   │   ├── config.model.ts
│   │   ├── issue.model.ts
│   │   ├── project.model.ts
│   │   └── sprint.model.ts
│   └── services/
│       ├── keycloak.service.ts   Keycloak wrapper: init, getToken, logout, user info
│       ├── comment.service.ts
│       ├── issue.service.ts
│       ├── project.service.ts
│       └── sprint.service.ts
├── features/
│   ├── projects/           /projects — list + create
│   ├── board/              /projects/:id/board — Kanban
│   ├── backlog/            /projects/:id/backlog — sprint management
│   └── issue-detail/       /issues/:id — full issue view + comments
└── layout/                 Shell: header, sidebar, <router-outlet>
```

---

## 8. Authentication Flow

### Step-by-step login

```
1.  User opens http://localhost:4200

2.  APP_INITIALIZER calls KeycloakService.init()
    → keycloak.init({ onLoad: 'login-required', checkLoginIframe: false })

3.  User is NOT authenticated yet
    → keycloak-js redirects the browser to Keycloak:
      http://localhost:8180/realms/jira-clone/protocol/openid-connect/auth
        ?client_id=jira-clone-app
        &redirect_uri=http://localhost:4200/
        &response_type=code

4.  User enters username + password on the Keycloak-hosted login page

5.  Keycloak validates credentials, issues auth code
    → browser redirected back to http://localhost:4200?code=...

6.  keycloak-js exchanges code for tokens:
    → access_token  (JWT, ~5 min lifetime)
    → refresh_token (~30 min lifetime)
    Tokens stored in memory by keycloak-js

7.  Angular app bootstraps, user sees the app

8.  Any HTTP request (e.g. GET /api/projects):
    auth.interceptor calls KeycloakService.getToken()
      → calls keycloak.updateToken(30)  [refreshes if expires within 30s]
      → returns current access_token
    Interceptor attaches: Authorization: Bearer <access_token>

9.  Spring Boot receives the request:
    → Fetches Keycloak JWKS (cached):
      GET http://localhost:8180/realms/jira-clone/protocol/openid-connect/certs
    → Verifies JWT signature, issuer, expiry
    → If valid: request proceeds → controller → service → DB → 200 OK
    → If invalid/missing: 401 Unauthorized

10. User logs out:
    keycloakService.logout()
    → keycloak.logout({ redirectUri: 'http://localhost:4200' })
    → Keycloak clears the session, redirects back to login
```

### Token lifecycle

| Token | Lifetime | Purpose |
|-------|----------|---------|
| access_token | ~5 minutes | Sent with every API request |
| refresh_token | ~30 minutes | Used to silently refresh access_token |
| Session | configurable | Keycloak server-side session |

`keycloak.updateToken(30)` is called before every request. If the access_token expires within 30 seconds, it is refreshed automatically using the refresh_token — no re-login needed.

---

## 9. Fallback Behaviour (Keycloak Down)

The app is hardened to survive Keycloak being unreachable:

| Layer | What happens |
|-------|-------------|
| `KeycloakService.init()` | `try/catch` — sets `isAvailable = false`, logs warning. Never throws. |
| `APP_INITIALIZER` | `.catch(() => {})` — Angular always bootstraps even if KC is down. |
| `KeycloakService.getToken()` | Returns `''` immediately if `isAvailable` is false. |
| `auth.interceptor` | `catchError(() => of(''))` — if token fetch fails, request is sent without Auth header. |
| Spring Boot | Returns `401 Unauthorized` for unauthenticated requests (app stays alive). |

**Result:** With Keycloak down, the Angular app loads and renders. API calls return 401 but the browser does not crash or hang.

### KeycloakService source (`keycloak.service.ts`)

```typescript
@Injectable({ providedIn: 'root' })
export class KeycloakService {
  private keycloak = new Keycloak({
    url: environment.keycloak.url,
    realm: environment.keycloak.realm,
    clientId: environment.keycloak.clientId
  });

  isAvailable = false;

  async init(): Promise<void> {
    try {
      await this.keycloak.init({ onLoad: 'login-required', checkLoginIframe: false });
      this.isAvailable = true;
    } catch {
      console.warn('Keycloak unavailable — running without authentication');
    }
  }

  getToken(): Promise<string> {
    if (!this.isAvailable) return Promise.resolve('');
    return this.keycloak.updateToken(30)
      .then(() => this.keycloak.token ?? '')
      .catch(() => '');
  }
  // ...
}
```

---

## 10. Backend Reference

### application.properties

```properties
server.port=8080
spring.application.name=jira-clone-backend

# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/jiraclone?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=sam9311
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA / Hibernate
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update        # auto-creates / migrates tables
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.jdbc.time_zone=UTC

# Jackson
spring.jackson.serialization.indent-output=false
spring.jackson.default-property-inclusion=non_null

# Keycloak JWT validation
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/jira-clone

# Logging
logging.level.com.jiraclone=INFO
```

### SecurityConfig.java

```java
http
  .cors(Customizer.withDefaults())          // CORS delegated to CorsConfig bean
  .csrf(csrf -> csrf.disable())             // REST API — no CSRF needed
  .authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()  // CORS preflight
    .anyRequest().authenticated()           // everything else needs JWT
  )
  .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
```

### Build and run

```bash
cd jira-clone-backend

# Compile only
mvn compile

# Start dev server
mvn spring-boot:run

# Build JAR
mvn package -DskipTests

# Run JAR directly
java -jar target/jira-clone-backend-1.0.0.jar
```

### Key Maven dependencies

| Dependency | Purpose |
|-----------|---------|
| spring-boot-starter-web | REST controllers |
| spring-boot-starter-data-jpa | ORM / Hibernate |
| mysql-connector-j | MySQL JDBC driver |
| spring-boot-starter-security | Security filter chain |
| spring-boot-starter-oauth2-resource-server | JWT validation |
| poi-ooxml 5.2.5 | Excel export |
| jsoup 1.17.2 | HTML → plain text for export |
| lombok | @Data, @Builder, etc. |

---

## 11. Frontend Reference

### npm scripts

```bash
npm start            # ng serve — dev server at http://localhost:4200
npm run build        # ng build — production build → dist/
npm test             # ng test — unit tests via Karma / Jasmine
npm run watch        # ng build --watch (incremental dev build)
```

> **Install note:** Use `npm install --legacy-peer-deps` due to ngx-quill peer dependency conflict with Angular 20.

### Routes

| Path | Component | Description |
|------|-----------|-------------|
| `/` | → redirect | Redirects to `/projects` |
| `/projects` | ProjectsComponent | List + create projects |
| `/projects/:projectId/board` | BoardComponent | Kanban board (drag & drop) |
| `/projects/:projectId/backlog` | BacklogComponent | Sprint planning + backlog |
| `/issues/:issueId` | IssueDetailComponent | Full issue view (inline edit, comments) |

All routes are wrapped in `LayoutComponent` (shell: header + nav + `<router-outlet>`).
All feature components are **lazy-loaded**.

### app.config.ts providers

| Provider | Purpose |
|---------|---------|
| `provideRouter(routes)` | Routing |
| `provideHttpClient(withInterceptors([authInterceptor]))` | HTTP + JWT interceptor |
| `provideAnimationsAsync()` | PrimeNG animations |
| `providePrimeNG({ theme: Aura })` | PrimeNG + Aura theme |
| `APP_INITIALIZER → kc.init()` | Keycloak login on app start |

### KeycloakService public API

| Method | Returns | Description |
|--------|---------|-------------|
| `init()` | `Promise<void>` | Initialise Keycloak; sets `isAvailable` flag |
| `getToken()` | `Promise<string>` | Refresh + return current JWT (`''` if KC down) |
| `logout()` | `void` | Log out + redirect to `:4200` |
| `getUsername()` | `string` | `preferred_username` from token |
| `getUserFullName()` | `string` | `name` claim from token |
| `getInitials()` | `string` | First letter of each name word, max 2 chars |
| `isAvailable` | `boolean` | `true` if Keycloak connected successfully |

---

## 12. API Endpoints

All endpoints require `Authorization: Bearer <JWT>` header.
Base URL: `http://localhost:8080/api`

### Projects

| Method | Path | Description |
|--------|------|-------------|
| GET | `/projects` | List all projects |
| POST | `/projects` | Create project |
| GET | `/projects/{id}` | Get project by ID |
| PUT | `/projects/{id}` | Update project |
| DELETE | `/projects/{id}` | Delete project |

### Issues

| Method | Path | Description |
|--------|------|-------------|
| GET | `/issues` | List issues (filter by project, sprint, status, etc.) |
| POST | `/issues` | Create issue |
| GET | `/issues/{id}` | Get issue detail |
| PUT | `/issues/{id}` | Update issue (summary, description, priority, assignee…) |
| DELETE | `/issues/{id}` | Delete issue |
| PATCH | `/issues/{id}/status` | Update status only |

### Sprints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/sprints` | List sprints (filter by project) |
| POST | `/sprints` | Create sprint |
| PUT | `/sprints/{id}` | Update sprint |
| POST | `/sprints/{id}/start` | Start sprint |
| POST | `/sprints/{id}/complete` | Complete sprint |
| POST | `/sprints/assign` | Assign issue to sprint |

### Comments

| Method | Path | Description |
|--------|------|-------------|
| GET | `/comments` | List comments for an issue |
| POST | `/comments` | Add comment |
| PUT | `/comments/{id}` | Edit comment |
| DELETE | `/comments/{id}` | Delete comment |

### Other

| Method | Path | Description |
|--------|------|-------------|
| GET | `/activity` | Activity log |
| GET | `/search` | Global search across issues |
| GET | `/export/{projectId}` | Export issues to Excel (.xlsx) |
| GET | `/config` | Read app configuration |
| PUT | `/config` | Update app configuration |

---

## 13. Environment Configuration

### Development — `environment.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  keycloak: {
    url: 'http://localhost:8180',
    realm: 'jira-clone',
    clientId: 'jira-clone-app'
  }
};
```

### Production — `environment.prod.ts`

```typescript
export const environment = {
  production: true,
  apiUrl: 'http://localhost:8080/api',   // ← update to production server URL
  keycloak: {
    url: 'http://localhost:8180',        // ← update to production Keycloak URL
    realm: 'jira-clone',
    clientId: 'jira-clone-app'
  }
};
```

Both files have the same structure; update URLs for a real deployment.

---

## 14. Common Commands Cheat Sheet

```bash
# ─── DOCKER / KEYCLOAK ────────────────────────────────────────────
docker compose up -d                     # Start Keycloak (background)
docker compose down                      # Stop Keycloak (keep data)
docker compose down -v                   # Stop + WIPE all realm data
docker compose ps                        # Status check
docker compose logs -f keycloak          # Live log stream
docker compose restart keycloak          # Restart without full down/up
docker ps                                # All running containers

# ─── BACKEND ──────────────────────────────────────────────────────
cd jira-clone-backend
mvn spring-boot:run                      # Start dev server
mvn compile                              # Compile only
mvn package -DskipTests                  # Build executable JAR
java -jar target/jira-clone-backend-1.0.0.jar

# ─── FRONTEND ─────────────────────────────────────────────────────
cd jira-clone-frontend
npm start                                # Dev server → http://localhost:4200
npm run build                            # Production build → dist/
npm test                                 # Unit tests
npm install --legacy-peer-deps           # Install (use this flag)

# ─── MYSQL ────────────────────────────────────────────────────────
mysql -u root -psam9311 jiraclone        # Open MySQL shell on jiraclone DB
mysql -u root -psam9311 -e "SHOW TABLES FROM jiraclone;"

# ─── KEYCLOAK HEALTH CHECK ────────────────────────────────────────
curl http://localhost:8180/realms/jira-clone
# Returns JSON → healthy; "connection refused" → not started yet
```

---

## 15. Startup Order

Services must start in this exact order:

```
1. MySQL          (native — must be running before anything else)
        ↓
2. Keycloak       docker compose up -d
   (wait ~10s for it to be ready)
        ↓
3. Spring Boot    mvn spring-boot:run
   (validates Keycloak issuer-uri on startup — fails if KC not ready)
        ↓
4. Angular        npm start
   (contacts Keycloak for login on first load)
```

### Why the order matters

- **Spring Boot** resolves `spring.security.oauth2.resourceserver.jwt.issuer-uri` at startup by fetching Keycloak's discovery document (`/.well-known/openid-configuration`). If Keycloak is not up, the backend fails to start.
- **Angular** redirects to Keycloak on first load. If Keycloak is down, the app still loads (fallback), but login will not work.

---

## 16. Troubleshooting

### Keycloak container won't start

```bash
docker compose logs keycloak
```

Common causes:

| Symptom | Fix |
|---------|-----|
| Port 8180 already in use | `netstat -ano \| findstr 8180` → kill the process |
| Docker Desktop not running | Start Docker Desktop first |
| Image not pulled | `docker pull quay.io/keycloak/keycloak:24.0.1` |

---

### Spring Boot fails: "Cannot obtain configuration from issuer-uri"

Keycloak must be fully started before Spring Boot. Wait for:
```bash
curl http://localhost:8180/realms/jira-clone
# Must return JSON, not error
```
Then start Spring Boot.

---

### Angular shows blank/white screen

Check the browser console (F12 → Console).

| Error message | Fix |
|---------------|-----|
| `Keycloak is not defined` | Reinstall: `npm install --legacy-peer-deps` |
| Network error to `:8180` | Keycloak not running — `docker compose up -d` |
| Realm not found (404) | Create realm `jira-clone` in admin console |
| Redirect URI mismatch | Ensure `http://localhost:4200/*` is in Client → Valid Redirect URIs |

---

### HTTP 401 Unauthorized from API

```
Cause 1: Token not sent
  → Open DevTools → Network tab → check Authorization header on the request

Cause 2: Token expired
  → Hard refresh (Ctrl+Shift+R), re-login

Cause 3: Issuer mismatch
  → application.properties issuer-uri must be exactly:
    http://localhost:8180/realms/jira-clone
  → Must match the issuer claim inside the JWT (decode at jwt.io)

Cause 4: CORS issue
  → Check CorsConfig.java allowed origins includes http://localhost:4200
```

---

### MySQL "Access denied" or "Connection refused"

```bash
# Check MySQL is running (Windows)
net start MySQL80

# Verify credentials match application.properties
mysql -u root -psam9311 -e "SELECT 1"
```

---

### npm install fails

```bash
npm install --legacy-peer-deps
```

This is required because `ngx-quill` has a peer dependency conflict with Angular 20.

---

### Keycloak ZIP files (fallback option)

Two Keycloak ZIPs are available at `C:\comic\`:
- `keycloak-24.0.0.zip`
- `keycloak-26.5.4.zip`

These are a **backup** in case Docker is unavailable. To use without Docker:

```bash
# Extract and run Keycloak 24 directly
cd C:\comic
unzip keycloak-24.0.0.zip
cd keycloak-24.0.0\bin
kc.bat start-dev --http-port=8180
```

Then set the `KEYCLOAK_ADMIN` and `KEYCLOAK_ADMIN_PASSWORD` environment variables before starting:

```cmd
set KEYCLOAK_ADMIN=admin
set KEYCLOAK_ADMIN_PASSWORD=admin
kc.bat start-dev --http-port=8180
```

> **Prefer Docker.** The ZIP approach requires manual realm setup after each restart (no persistence by default). With Docker Compose, realm data persists in a named volume.

---

*End of documentation*
