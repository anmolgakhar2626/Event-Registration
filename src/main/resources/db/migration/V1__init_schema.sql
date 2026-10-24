CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    is_verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE events (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    organiser_id       BIGINT         NOT NULL,
    title              VARCHAR(200)   NOT NULL,
    description        TEXT,
    venue              VARCHAR(255)   NOT NULL,
    event_date         DATETIME       NOT NULL,
    max_capacity       INT            NOT NULL,
    waitlist_cap       INT            NOT NULL DEFAULT 0,
    price              DECIMAL(10,2)  NOT NULL DEFAULT 0.00,
    status             VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    is_featured        BOOLEAN        NOT NULL DEFAULT FALSE,
    refund_eligible    BOOLEAN        NOT NULL DEFAULT FALSE,
    is_deleted         BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_events_organiser FOREIGN KEY (organiser_id) REFERENCES users(id),
    CONSTRAINT chk_events_capacity CHECK (max_capacity > 0),
    INDEX idx_events_status_date (status, event_date),
    INDEX idx_events_organiser (organiser_id)
) ENGINE=InnoDB;

CREATE TABLE team_registrations (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    lead_user_id  BIGINT      NOT NULL,
    event_id      BIGINT      NOT NULL,
    member_count  INT         NOT NULL,
    group_token   VARCHAR(36) NOT NULL,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_team_lead  FOREIGN KEY (lead_user_id) REFERENCES users(id),
    CONSTRAINT fk_team_event FOREIGN KEY (event_id)     REFERENCES events(id),
    CONSTRAINT uk_team_group_token UNIQUE (group_token)
) ENGINE=InnoDB;

CREATE TABLE registrations (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id              BIGINT      NOT NULL,
    event_id             BIGINT      NOT NULL,
    status               VARCHAR(20) NOT NULL,
    team_registration_id BIGINT      NULL,
    registered_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reg_user  FOREIGN KEY (user_id)  REFERENCES users(id),
    CONSTRAINT fk_reg_event FOREIGN KEY (event_id) REFERENCES events(id),
    CONSTRAINT fk_reg_team  FOREIGN KEY (team_registration_id) REFERENCES team_registrations(id),
    CONSTRAINT uk_reg_user_event UNIQUE (user_id, event_id),
    INDEX idx_reg_event_status (event_id, status, registered_at)
) ENGINE=InnoDB;

CREATE TABLE tickets (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    registration_id BIGINT      NOT NULL,
    token           VARCHAR(36) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    issued_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    checked_in_at   TIMESTAMP   NULL,
    CONSTRAINT fk_ticket_registration FOREIGN KEY (registration_id) REFERENCES registrations(id),
    CONSTRAINT uk_ticket_token UNIQUE (token),
    CONSTRAINT uk_ticket_registration UNIQUE (registration_id)
) ENGINE=InnoDB;

CREATE TABLE email_retry_queue (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient     VARCHAR(150) NOT NULL,
    subject       VARCHAR(255) NOT NULL,
    body          TEXT         NOT NULL,
    attempts      INT          NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    INDEX idx_retry_status_next (status, next_retry_at)
) ENGINE=InnoDB;
