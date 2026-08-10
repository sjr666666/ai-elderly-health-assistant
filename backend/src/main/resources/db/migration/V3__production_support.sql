CREATE TABLE IF NOT EXISTS notification_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(64) NOT NULL,
    aggregate_id BIGINT NULL,
    user_id BIGINT NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME NULL,
    sent_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_outbox_event (event_type, aggregate_id, user_id),
    KEY idx_outbox_pending (status, next_retry_at, created_at)
);

CREATE TABLE IF NOT EXISTS async_task_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_type VARCHAR(32) NOT NULL,
    business_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_async_task (task_type, business_id),
    KEY idx_async_task_status (status, updated_at)
);

CREATE TABLE IF NOT EXISTS auth_refresh_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    token_hash CHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_refresh_hash (token_hash),
    KEY idx_refresh_user (user_id, revoked_at)
);
