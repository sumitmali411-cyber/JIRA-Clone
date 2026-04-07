-- =============================================================================
-- V1__initial_schema.sql
-- Full initial schema for JIRA Clone — all tables, indexes, and constraints.
-- MySQL 8.0 / InnoDB / utf8mb4
-- Entities: Project, Sprint, Issue, Comment, ActivityLog, AppConfig,
--           webhook_deliveries (audit table, no JPA entity)
-- =============================================================================

CREATE TABLE IF NOT EXISTS projects (
    id              VARCHAR(36)   NOT NULL,
    name            VARCHAR(255)  NOT NULL,
    project_key     VARCHAR(20)   NOT NULL,
    description     TEXT,
    type            ENUM('SCRUM','KANBAN'),
    statuses        TEXT,
    issue_counter   BIGINT        NOT NULL DEFAULT 0,
    created_at      DATETIME(6),
    updated_at      DATETIME(6),
    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT uq_projects_key UNIQUE (project_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_projects_key ON projects (project_key);

-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS sprints (
    id              VARCHAR(36)   NOT NULL,
    project_id      VARCHAR(36)   NOT NULL,
    name            VARCHAR(255)  NOT NULL,
    goal            TEXT,
    status          ENUM('PLANNED','ACTIVE','COMPLETED'),
    start_date      VARCHAR(20),
    end_date        VARCHAR(20),
    completed_at    DATETIME(6),
    created_at      DATETIME(6),
    updated_at      DATETIME(6),
    CONSTRAINT pk_sprints PRIMARY KEY (id),
    CONSTRAINT fk_sprints_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_sprints_project_id ON sprints (project_id);
CREATE INDEX idx_sprints_status     ON sprints (status);

-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS issues (
    id              VARCHAR(36)   NOT NULL,
    project_id      VARCHAR(36)   NOT NULL,
    issue_key       VARCHAR(50),
    issue_number    BIGINT        NOT NULL DEFAULT 0,
    type            ENUM('EPIC','STORY','TASK','BUG','SUB_TASK'),
    status          VARCHAR(100),
    priority        ENUM('HIGHEST','HIGH','MEDIUM','LOW','LOWEST'),
    summary         VARCHAR(500)  NOT NULL,
    description     TEXT,
    assignee        VARCHAR(255),
    reporter        VARCHAR(255),
    sprint_id       VARCHAR(36),
    epic_id         VARCHAR(36),
    parent_id       VARCHAR(36),
    labels          TEXT,
    story_points    INT,
    time_estimate   VARCHAR(50),
    due_date        VARCHAR(20),
    links           TEXT,
    git_commits     TEXT,
    created_at      DATETIME(6),
    updated_at      DATETIME(6),
    CONSTRAINT pk_issues PRIMARY KEY (id),
    CONSTRAINT fk_issues_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_issues_project_id ON issues (project_id);
CREATE INDEX idx_issues_sprint_id  ON issues (sprint_id);
CREATE INDEX idx_issues_status     ON issues (status);
CREATE INDEX idx_issues_assignee   ON issues (assignee);
CREATE INDEX idx_issues_issue_key  ON issues (issue_key);
ALTER TABLE issues ADD FULLTEXT INDEX ft_issues_search (summary, description);

-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS comments (
    id          VARCHAR(36)  NOT NULL,
    issue_id    VARCHAR(36)  NOT NULL,
    author      VARCHAR(255),
    body        TEXT,
    edited      TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  DATETIME(6),
    updated_at  DATETIME(6),
    CONSTRAINT pk_comments PRIMARY KEY (id),
    CONSTRAINT fk_comments_issue FOREIGN KEY (issue_id)
        REFERENCES issues(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_comments_issue_id ON comments (issue_id);

-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS activity_logs (
    id          VARCHAR(36)  NOT NULL,
    issue_id    VARCHAR(36),
    project_id  VARCHAR(36),
    actor       VARCHAR(255),
    field       VARCHAR(100),
    old_value   TEXT,
    new_value   TEXT,
    action      VARCHAR(100),
    timestamp   DATETIME(6),
    CONSTRAINT pk_activity_logs PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_activity_logs_issue_id  ON activity_logs (issue_id);
CREATE INDEX idx_activity_logs_timestamp ON activity_logs (timestamp);

-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS app_config (
    id               BIGINT       NOT NULL DEFAULT 1,
    labels           TEXT,
    default_assignee VARCHAR(255),
    default_reporter VARCHAR(255),
    team_members     TEXT,
    CONSTRAINT pk_app_config PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- webhook_deliveries: audit log for incoming GitHub webhook events.
-- No JPA entity — written directly by GitHubWebhookController if needed later.

CREATE TABLE IF NOT EXISTS webhook_deliveries (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    event_type      VARCHAR(50)  NOT NULL,
    delivery_sha    VARCHAR(64),
    repo_name       VARCHAR(255),
    branch          VARCHAR(255),
    commit_count    INT          NOT NULL DEFAULT 0,
    status          ENUM('PROCESSED','IGNORED','FAILED','INVALID_SIGNATURE') NOT NULL,
    error_msg       TEXT,
    received_at     DATETIME(6)  NOT NULL,
    CONSTRAINT pk_webhook_deliveries PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_webhook_deliveries_received_at ON webhook_deliveries (received_at);
CREATE INDEX idx_webhook_deliveries_status      ON webhook_deliveries (status);
