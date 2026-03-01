# JIRA Clone — Frontend

Angular 20 SPA with PrimeNG 20 (Aura theme), consuming the Spring Boot REST API.

## Quick Start

```bash
npm install
npm start          # dev server at http://localhost:4200
npm run build      # production build
```

## App Structure

```mermaid
graph TB
    APP["AppComponent<br/>app.ts"] --> LAYOUT["LayoutComponent<br/>Collapsible Sidebar"]

    LAYOUT --> R1["/projects<br/>ProjectsComponent"]
    LAYOUT --> R2["/projects/:id/board<br/>BoardComponent"]
    LAYOUT --> R3["/projects/:id/backlog<br/>BacklogComponent"]
    LAYOUT --> R4["/issues/:id<br/>IssueDetailComponent"]

    subgraph Services["Core Services (providedIn: root)"]
        PS["ProjectService"]
        IS["IssueService"]
        SS["SprintService"]
        CS["CommentService"]
    end

    R1 & R2 & R3 & R4 --> Services
    Services -->|"HttpClient"| API["Spring Boot :8080"]
```

## Routing

| Path | Component | Description |
|---|---|---|
| `/` | redirect | → `/projects` |
| `/projects` | ProjectsComponent | Project cards grid |
| `/projects/:id/board` | BoardComponent | Kanban board |
| `/projects/:id/backlog` | BacklogComponent | Sprint + backlog |
| `/issues/:id` | IssueDetailComponent | Issue detail/edit |

## Component Map

```mermaid
graph TD
    subgraph Layout
        SIDEBAR["Sidebar Nav<br/>project links"]
        TOPBAR["Top Bar<br/>user avatar"]
    end

    subgraph ProjectsPage["Projects Page"]
        PGRID["Project Cards Grid"]
        PDIALOG["Create Project Dialog"]
    end

    subgraph BoardPage["Board Page"]
        COLS["Kanban Columns<br/>(drag & drop)"]
        CARDS["Issue Cards"]
        BDIALOG["Create Issue Dialog"]
    end

    subgraph BacklogPage["Backlog Page"]
        SPRINTS["Sprint Sections<br/>start/complete"]
        BLIST["Issue Rows"]
        SDIALOG["Create Sprint Dialog"]
        IDIALOG["Create Issue Dialog"]
    end

    subgraph IssuePage["Issue Detail Page"]
        SUMMARY["Inline Edit Summary"]
        DESC["Inline Edit Description"]
        COMS["Comments Section"]
        DBAR["Details Sidebar<br/>status · priority · assignee"]
    end

    SIDEBAR --> ProjectsPage & BoardPage & BacklogPage
```

## PrimeNG Components Used

| PrimeNG | Usage |
|---|---|
| `p-button` | All action buttons |
| `p-dialog` | Create forms (project, issue, sprint) |
| `p-select` | Dropdowns (type, priority, status) |
| `p-inputtext` | Text inputs |
| `p-textarea` | Description / comment fields |
| `p-toast` | Success/error notifications |
| `p-tag` | Status badges |
| `p-progressspinner` | Loading indicators |
| `p-avatar` | User avatars |
| `p-divider` | Section separators |
| `p-accordion` | Collapsible sprint sections |

## State Flow

```mermaid
stateDiagram-v2
    [*] --> ProjectsList
    ProjectsList --> BoardView : click project
    ProjectsList --> BacklogView : click project backlog
    BoardView --> IssueDetail : click issue card
    BacklogView --> IssueDetail : click issue row
    BoardView --> BacklogView : click Backlog button
    BacklogView --> BoardView : click Board button
    IssueDetail --> BoardView : click Back

    state BoardView {
        [*] --> LoadingData
        LoadingData --> ShowColumns
        ShowColumns --> DragDrop : drag card
        DragDrop --> UpdateAPI
        UpdateAPI --> ShowColumns
    }
```

## Environment Config

`src/environments/environment.ts`

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

## Scripts

```bash
npm start          # ng serve (dev, port 4200)
npm run build      # ng build (production)
npm test           # ng test (karma)
```
