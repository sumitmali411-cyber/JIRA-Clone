# Docker & Keycloak — How It Works

This guide explains Docker, how it's used in this project, and how to manage Keycloak (the authentication server) that runs inside Docker.

---

## What is Docker?

Docker is a tool that lets you run software inside **containers** — isolated, self-contained environments that behave the same way on any machine.

Think of a container like a **mini virtual machine**, but much lighter. Instead of installing Keycloak directly on your computer (extracting the zip, configuring it, running it), Docker downloads and runs it for you in seconds, and you can throw it away when you don't need it.

```
Without Docker:                     With Docker:
──────────────                      ────────────
1. Download Keycloak zip            1. docker compose up -d
2. Extract it                          └─ That's it.
3. Configure Java
4. Edit config files
5. Run startup script
6. Hope it works
```

---

## How Docker Is Used In This Project

This project uses Docker **only for Keycloak** (the authentication server). The backend (Spring Boot) and frontend (Angular) still run directly on your machine.

```
┌─────────────────────────────────────────────────────┐
│                   Your Machine                       │
│                                                      │
│  ┌──────────────┐    ┌──────────────┐               │
│  │   Angular    │    │ Spring Boot  │               │
│  │  :4200       │    │  :8080       │               │
│  └──────┬───────┘    └──────┬───────┘               │
│         │                   │                        │
│         │         ┌─────────▼──────────┐            │
│         └────────►│  Docker Container  │            │
│                   │  Keycloak :8180    │            │
│                   └────────────────────┘            │
└─────────────────────────────────────────────────────┘
```

### Why Keycloak in Docker?

- Keycloak is a complex Java server — running it via Docker avoids manual setup
- The container is pre-configured and ready instantly
- You can start/stop it with one command

---

## The docker-compose.yml File

Located at the **project root**: `docker-compose.yml`

```yaml
services:
  keycloak:
    image: quay.io/keycloak/keycloak:24.0.1   # Which version of Keycloak to use
    command: start-dev                          # Run in development mode (no HTTPS required)
    environment:
      KEYCLOAK_ADMIN: admin                     # Admin username
      KEYCLOAK_ADMIN_PASSWORD: admin            # Admin password
    ports:
      - "8180:8080"                             # Map your machine's 8180 → container's 8080
```

### What each line means

| Line | What it does |
|------|-------------|
| `image: quay.io/keycloak/keycloak:24.0.1` | Downloads Keycloak v24 from the internet (once, then cached) |
| `command: start-dev` | Runs Keycloak in dev mode — no SSL, faster startup |
| `KEYCLOAK_ADMIN: admin` | Creates an admin account with username `admin` |
| `KEYCLOAK_ADMIN_PASSWORD: admin` | Sets admin password to `admin` |
| `"8180:8080"` | Keycloak runs on port 8080 inside the container. We expose it as 8180 on your machine so it doesn't clash with Spring Boot (also on 8080) |

---

## Keycloak Configuration (Already Done)

When you ran `docker compose up` for the first time, these were set up via the Admin REST API:

| Item | Value |
|------|-------|
| Realm | `jira-clone` |
| Client | `jira-clone-app` (public SPA) |
| Redirect URI | `http://localhost:4200/*` |
| Test User | `testuser` / `password` |

### How It Connects to the App

```
Angular (localhost:4200)
  └─► keycloak-js library
        └─► Redirects to Keycloak login (localhost:8180/realms/jira-clone)
              └─► User logs in → Keycloak issues a JWT token
                    └─► Angular stores token, sends it on every API call

Spring Boot (localhost:8080)
  └─► spring-security oauth2-resource-server
        └─► Validates JWT token against Keycloak
              └─► If valid → allow request
              └─► If missing/invalid → 401 Unauthorized
```

---

## Common Docker Commands

Run these from the **project root** (where `docker-compose.yml` is).

### Start Keycloak
```bash
docker compose up -d
```
- `-d` means "detached" — runs in the background, you get your terminal back

### Stop Keycloak
```bash
docker compose down
```

### Stop and delete all data (reset Keycloak)
```bash
docker compose down -v
```
> Warning: This deletes the realm, users, and all Keycloak config. You'd need to recreate them.

### Check if it's running
```bash
docker ps
```

### View Keycloak logs
```bash
docker compose logs -f keycloak
```
- `-f` means "follow" — streams live logs. Press `Ctrl+C` to stop.

### Restart Keycloak
```bash
docker compose restart keycloak
```

---

## Keycloak Admin Console

Once running, go to: **http://localhost:8180/admin**

Login: `admin` / `admin`

From here you can:
- View/edit the `jira-clone` realm
- Add/remove users
- Configure the `jira-clone-app` client
- View sessions and tokens

---

## Startup Order (Important)

Always start services in this order:

```
1. MySQL          (must be running first — Spring Boot connects on startup)
2. Docker/Keycloak   docker compose up -d
3. Spring Boot    cd jira-clone-backend && mvn spring-boot:run
4. Angular        cd jira-clone-frontend && npm start
```

If Spring Boot starts before Keycloak is ready, it will fail to fetch the JWT public keys and crash. Wait ~10 seconds after starting Keycloak before starting Spring Boot.

---

## Ports Summary

| Service | Port | URL |
|---------|------|-----|
| Angular (Frontend) | 4200 | http://localhost:4200 |
| Spring Boot (Backend) | 8080 | http://localhost:8080 |
| Keycloak (Docker) | 8180 | http://localhost:8180 |
| MySQL | 3306 | localhost:3306 |

---

## Troubleshooting

### "Keycloak not reachable" / Spring Boot fails to start
```bash
# Check if container is running
docker ps

# If not running, start it
docker compose up -d

# Wait ~15 seconds, then start Spring Boot
```

### "Invalid token" errors in Angular
- Make sure Keycloak is running: `docker ps`
- Open http://localhost:8180/realms/jira-clone — should return JSON
- Try clearing browser storage and refreshing

### Keycloak container keeps restarting
```bash
# Check logs for the error
docker compose logs keycloak
```

### Want to add a new user?
```bash
# Via Admin Console (easiest)
# Go to http://localhost:8180/admin → jira-clone realm → Users → Add User
```
