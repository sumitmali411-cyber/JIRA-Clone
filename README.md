# JIRA Clone

A full-stack project management application inspired by Atlassian JIRA, built with **Spring Boot + MySQL** on the backend and **Angular 20 + PrimeNG** on the frontend.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture Overview](#architecture-overview)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
- [Database Schema](#database-schema)

---

## Features

| Feature | Description |
|---|---|
| Projects | Create and manage Scrum/Kanban projects |
| Kanban Board | Drag & drop issues across status columns |
| Backlog | Sprint planning with issue assignment |
| Sprint Management | Create, start, and complete sprints |
| Issue Tracking | Full CRUD with type, priority, assignee, labels |
| Comments | Threaded comments on issues |
| Activity Log | Audit trail of all issue changes |
| Export | Export issue list to Excel |
| Search | Filter issues by type, status, priority, assignee |

---

## Tech Stack

### Backend
- **Java 17** + **Spring Boot 3.2.3**
- **Spring Data JPA** + **Hibernate**
- **MySQL 8.0**
- **Apache POI** (Excel export)
- **Lombok**

### Frontend
- **Angular 20** (Standalone components)
- **PrimeNG 20** (UI component library, Aura theme)
- **TypeScript 5.8**

---

## Architecture Overview

### High-Level System Architecture

```mermaid
graph TB
    subgraph Browser["Browser (Angular 20)"]
        UI["Angular SPA<br/>PrimeNG Components"]
    end

    subgraph Backend["Backend (Spring Boot 3.2.3)"]
        CTRL["REST Controllers<br/>Port 8080"]
        SVC["Service Layer"]
        REPO["JPA Repositories"]
    end

    subgraph DB["Database (MySQL 8.0)"]
        TABLES["Tables:<br/>projects · issues · sprints<br/>comments · activity_logs · app_config"]
    end

    UI -->|"HTTP/REST API<br/>JSON"| CTRL
    CTRL --> SVC
    SVC --> REPO
    REPO -->|"JDBC / Hibernate"| TABLES
```

### Backend Layer Architecture

```mermaid
graph LR
    subgraph Controllers
        PC[ProjectController]
        IC[IssueController]
        SC[SprintController]
        CC[CommentController]
        AC[ActivityController]
        XC[ExportController]
        SRC[SearchController]
        CFG[ConfigController]
    end

    subgraph Services
        PS[ProjectService]
        IS[IssueService]
        SS[SprintService]
        CS[CommentService]
        AS[ActivityService]
        ES[ExportService]
        SS2[SearchService]
        CS2[ConfigService]
    end

    subgraph Repositories
        PR[ProjectRepository]
        IR[IssueRepository]
        SPR[SprintRepository]
        CR[CommentRepository]
        AR[ActivityLogRepository]
        APR[AppConfigRepository]
    end

    subgraph MySQL
        DB[(MySQL 8.0<br/>jiraclone)]
    end

    PC --> PS
    IC --> IS
    SC --> SS
    CC --> CS
    AC --> AS
    XC --> ES
    SRC --> SS2
    CFG --> CS2

    PS --> PR
    IS --> IR
    SS --> SPR
    CS --> CR
    AS --> AR
    ES --> SPR
    SS2 --> IR
    CS2 --> APR

    PR & IR & SPR & CR & AR & APR --> DB
```

### Frontend Architecture

```mermaid
graph TB
    subgraph App["Angular Application"]
        ROOT["AppComponent<br/>(Root)"]
        LAYOUT["LayoutComponent<br/>(Sidebar + Header)"]

        subgraph Pages["Feature Pages (Lazy Loaded)"]
            PROJ["ProjectsComponent<br/>/projects"]
            BOARD["BoardComponent<br/>/projects/:id/board"]
            BACKLOG["BacklogComponent<br/>/projects/:id/backlog"]
            DETAIL["IssueDetailComponent<br/>/issues/:id"]
        end

        subgraph Services["Core Services"]
            PS["ProjectService"]
            IS["IssueService"]
            SS["SprintService"]
            CS["CommentService"]
        end
    end

    API["Spring Boot REST API<br/>localhost:8080"]

    ROOT --> LAYOUT
    LAYOUT --> Pages
    Pages --> Services
    Services -->|"HttpClient"| API
```

### Data Flow — Issue Status Update (Drag & Drop)

```mermaid
sequenceDiagram
    participant User
    participant Board as Board Component
    participant IssueService
    participant API as Spring REST API
    participant DB as MySQL

    User->>Board: Drag issue card to new column
    Board->>Board: onDrop(newStatus) — update UI optimistically
    Board->>IssueService: updateStatus(issueId, {status})
    IssueService->>API: PATCH /api/issues/:id/status
    API->>DB: UPDATE issues SET status=? WHERE id=?
    DB-->>API: OK
    API-->>IssueService: Updated Issue JSON
    IssueService-->>Board: Observable<Issue>
    Note over Board: UI already updated (optimistic)
    Note over Board: On error: rollback to old status
```

### Data Flow — Create Issue

```mermaid
sequenceDiagram
    participant User
    participant Board as Board / Backlog
    participant API as Spring REST API
    participant ProjectSvc as ProjectService
    participant IssueSvc as IssueService
    participant DB as MySQL

    User->>Board: Fill form + click Create
    Board->>API: POST /api/projects/:id/issues
    API->>ProjectSvc: nextIssueNumber(projectId) [synchronized]
    ProjectSvc->>DB: SELECT + UPDATE projects.issue_counter
    DB-->>ProjectSvc: counter = N
    API->>IssueSvc: create(projectId, dto)
    IssueSvc->>DB: INSERT INTO issues (KEY-N, ...)
    DB-->>IssueSvc: Saved Issue
    API-->>Board: Issue JSON {issueKey: "PROJ-N"}
    Board->>Board: Append card to correct column
```

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- Node.js 20+
- MySQL 8.0 running locally

### 1. Start the Backend

```bash
cd jira-clone-backend

# MySQL must be running — the DB is auto-created
mvn spring-boot:run
```

Backend runs on **http://localhost:8080**
Tables are auto-created by Hibernate (`ddl-auto=update`)

### 2. Start the Frontend

```bash
cd jira-clone-frontend
npm install
npm start
```

Frontend runs on **http://localhost:4200**

---

## API Reference

Base URL: `http://localhost:8080/api`

### Projects

| Method | Endpoint | Description |
|---|---|---|
| GET | `/projects` | List all projects |
| POST | `/projects` | Create project |
| GET | `/projects/:id` | Get project |
| PUT | `/projects/:id` | Update project |
| DELETE | `/projects/:id` | Delete project |
| GET | `/projects/:id/issues` | Issues in project |
| GET | `/projects/:id/sprints` | Sprints in project |

### Issues

| Method | Endpoint | Description |
|---|---|---|
| GET | `/issues/:id` | Get issue |
| PUT | `/issues/:id` | Update issue |
| DELETE | `/issues/:id` | Delete issue |
| PATCH | `/issues/:id/status` | Update status |
| PATCH | `/issues/:id/sprint` | Assign to sprint |
| GET | `/issues/:id/comments` | Get comments |
| POST | `/issues/:id/comments` | Add comment |
| GET | `/issues/:id/activity` | Activity log |

### Sprints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/projects/:id/sprints` | Create sprint |
| PUT | `/sprints/:id` | Update sprint |
| POST | `/sprints/:id/start` | Start sprint |
| POST | `/sprints/:id/complete` | Complete sprint |
| DELETE | `/sprints/:id` | Delete sprint |

### Search & Export

| Method | Endpoint | Description |
|---|---|---|
| POST | `/search` | Search issues with filters |
| POST | `/export/excel` | Export to Excel |

---

## Database Schema

```mermaid
erDiagram
    projects {
        VARCHAR id PK
        VARCHAR name
        VARCHAR key UK
        TEXT description
        VARCHAR type
        TEXT statuses
        BIGINT issue_counter
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    issues {
        VARCHAR id PK
        VARCHAR project_id FK
        VARCHAR issue_key
        BIGINT issue_number
        VARCHAR type
        VARCHAR status
        VARCHAR priority
        VARCHAR summary
        TEXT description
        VARCHAR assignee
        VARCHAR reporter
        VARCHAR sprint_id FK
        VARCHAR epic_id
        VARCHAR parent_id
        TEXT labels
        INT story_points
        VARCHAR time_estimate
        VARCHAR due_date
        TEXT links
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    sprints {
        VARCHAR id PK
        VARCHAR project_id FK
        VARCHAR name
        TEXT goal
        VARCHAR status
        VARCHAR start_date
        VARCHAR end_date
        TIMESTAMP completed_at
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    comments {
        VARCHAR id PK
        VARCHAR issue_id FK
        VARCHAR author
        TEXT body
        BOOLEAN edited
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    activity_logs {
        VARCHAR id PK
        VARCHAR issue_id FK
        VARCHAR project_id
        VARCHAR actor
        VARCHAR field
        TEXT old_value
        TEXT new_value
        VARCHAR action
        TIMESTAMP timestamp
    }

    app_config {
        BIGINT id PK
        TEXT labels
        VARCHAR default_assignee
        VARCHAR default_reporter
        TEXT team_members
    }

    projects ||--o{ issues : "has"
    projects ||--o{ sprints : "has"
    issues ||--o{ comments : "has"
    issues ||--o{ activity_logs : "tracks"
    sprints ||--o{ issues : "contains"
```
