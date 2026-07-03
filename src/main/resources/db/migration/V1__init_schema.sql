-- ============================================================================
--  AzTU Support — initial schema
-- ============================================================================

-- ── Roles ───────────────────────────────────────────────────────────────────
CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- ── Users ───────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id               BIGSERIAL PRIMARY KEY,
    first_name       VARCHAR(100) NOT NULL,
    last_name        VARCHAR(100) NOT NULL,
    email            VARCHAR(255) NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    role_id          BIGINT       NOT NULL REFERENCES roles (id),
    status           VARCHAR(30)  NOT NULL DEFAULT 'PENDING_APPROVAL',
    email_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    approved_by      BIGINT       REFERENCES users (id),
    approved_at      TIMESTAMPTZ,
    rejection_reason VARCHAR(500),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_users_role   ON users (role_id);
CREATE INDEX idx_users_status ON users (status);

-- ── Platforms ───────────────────────────────────────────────────────────────
CREATE TABLE platforms (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(150) NOT NULL UNIQUE,
    description   VARCHAR(500),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    is_additional BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ── Categories (belong to a platform; "Additional" has its own sub-categories) ─
CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    platform_id BIGINT       NOT NULL REFERENCES platforms (id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (platform_id, name)
);
CREATE INDEX idx_categories_platform ON categories (platform_id);

-- ── Tickets ─────────────────────────────────────────────────────────────────
CREATE SEQUENCE ticket_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE tickets (
    id              BIGSERIAL PRIMARY KEY,
    ticket_number   VARCHAR(30)  NOT NULL UNIQUE,
    subject         VARCHAR(255) NOT NULL,
    description     TEXT         NOT NULL,
    priority        VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    platform_id     BIGINT       NOT NULL REFERENCES platforms (id),
    category_id     BIGINT       REFERENCES categories (id),
    created_by      BIGINT       NOT NULL REFERENCES users (id),
    assigned_to     BIGINT       REFERENCES users (id),
    resolution_note TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    closed_at       TIMESTAMPTZ
);
CREATE INDEX idx_tickets_created_by  ON tickets (created_by);
CREATE INDEX idx_tickets_assigned_to ON tickets (assigned_to);
CREATE INDEX idx_tickets_status      ON tickets (status);
CREATE INDEX idx_tickets_platform    ON tickets (platform_id);

-- ── Ticket status history (audit log of status transitions) ─────────────────
CREATE TABLE ticket_status_history (
    id          BIGSERIAL PRIMARY KEY,
    ticket_id   BIGINT      NOT NULL REFERENCES tickets (id) ON DELETE CASCADE,
    from_status VARCHAR(30),
    to_status   VARCHAR(30) NOT NULL,
    reason      TEXT,
    changed_by  BIGINT      NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_status_history_ticket ON ticket_status_history (ticket_id);

-- ── Comments (user <-> support thread) ──────────────────────────────────────
CREATE TABLE comments (
    id         BIGSERIAL PRIMARY KEY,
    ticket_id  BIGINT      NOT NULL REFERENCES tickets (id) ON DELETE CASCADE,
    author_id  BIGINT      NOT NULL REFERENCES users (id),
    body       TEXT        NOT NULL,
    internal   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_comments_ticket ON comments (ticket_id);

-- ── Attachments ─────────────────────────────────────────────────────────────
CREATE TABLE attachments (
    id                BIGSERIAL PRIMARY KEY,
    ticket_id         BIGINT       NOT NULL REFERENCES tickets (id) ON DELETE CASCADE,
    comment_id        BIGINT       REFERENCES comments (id) ON DELETE CASCADE,
    uploaded_by       BIGINT       NOT NULL REFERENCES users (id),
    original_filename VARCHAR(255) NOT NULL,
    stored_filename   VARCHAR(255) NOT NULL,
    content_type      VARCHAR(150),
    file_size         BIGINT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_attachments_ticket ON attachments (ticket_id);

-- ── Notifications (in-dashboard, with read/unread state) ────────────────────
CREATE TABLE notifications (
    id           BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type         VARCHAR(60)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    message      TEXT         NOT NULL,
    link         VARCHAR(255),
    ticket_id    BIGINT       REFERENCES tickets (id) ON DELETE CASCADE,
    is_read      BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at      TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_recipient ON notifications (recipient_id, is_read);

-- ── Auth tokens ─────────────────────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE password_reset_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE email_verification_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
