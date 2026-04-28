-- ============================================================
-- Invest Alert - Database Schema
-- MySQL DDL script
-- ============================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user` (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS asset (
    id              BIGINT         NOT NULL AUTO_INCREMENT,
    ticker          VARCHAR(20)    NOT NULL,
    name            VARCHAR(255)   NOT NULL,
    current_price   DECIMAL(19,4)  NOT NULL,
    dividend_yield  DECIMAL(19,4)  NOT NULL,
    p_vp            DECIMAL(19,4)  NOT NULL,
    updated_at      DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_asset_ticker UNIQUE (ticker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rule_group (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    ticker      VARCHAR(20)  NOT NULL,
    name        VARCHAR(255) NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_rule_group_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    INDEX idx_rule_group_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS rule (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    user_id       BIGINT         NOT NULL,
    ticker        VARCHAR(20)    NOT NULL,
    group_id      BIGINT         NULL,
    field         VARCHAR(30)    NOT NULL,
    operator      VARCHAR(30)    NOT NULL,
    target_value  DECIMAL(19,4)  NOT NULL,
    active        BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)    NOT NULL,
    updated_at    DATETIME(6)    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_rule_user   FOREIGN KEY (user_id)  REFERENCES `user` (id),
    CONSTRAINT fk_rule_asset  FOREIGN KEY (ticker)    REFERENCES asset (ticker),
    CONSTRAINT fk_rule_group  FOREIGN KEY (group_id)  REFERENCES rule_group (id),
    INDEX idx_rule_user_id (user_id),
    INDEX idx_rule_active_group (active, group_id),
    INDEX idx_rule_ticker (ticker)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS alert (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    rule_id     BIGINT       NULL,
    group_id    BIGINT       NULL,
    ticker      VARCHAR(20)  NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    details     TEXT         NULL,
    created_at  DATETIME(6)  NOT NULL,
    sent_at     DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_alert_user  FOREIGN KEY (user_id)  REFERENCES `user` (id),
    CONSTRAINT fk_alert_rule  FOREIGN KEY (rule_id)   REFERENCES rule (id),
    CONSTRAINT fk_alert_group FOREIGN KEY (group_id)  REFERENCES rule_group (id),
    INDEX idx_alert_user_created  (user_id, created_at DESC),
    INDEX idx_alert_user_ticker   (user_id, ticker, created_at DESC),
    INDEX idx_alert_user_status   (user_id, status, created_at DESC),
    INDEX idx_alert_status        (status),
    INDEX idx_alert_rule_dedup    (rule_id, ticker, status),
    INDEX idx_alert_group_dedup   (group_id, ticker, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
