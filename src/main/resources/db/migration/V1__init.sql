-- Users table
CREATE TABLE users (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username         VARCHAR(50)  NOT NULL,
    display_name     VARCHAR(100),
    password_hash    VARCHAR(255) NOT NULL,
    role             VARCHAR(10)  NOT NULL,
    parent_id        BIGINT,
    enabled          BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_attempts INT     NOT NULL DEFAULT 0,
    lockout_until    TIMESTAMP,
    created_at       TIMESTAMP    NOT NULL
);

ALTER TABLE users ADD CONSTRAINT uq_users_username UNIQUE (username);
ALTER TABLE users ADD CONSTRAINT fk_users_parent FOREIGN KEY (parent_id) REFERENCES users(id);

-- Savings accounts table
CREATE TABLE savings_accounts (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    child_user_id               BIGINT         NOT NULL,
    parent_user_id              BIGINT         NOT NULL,
    balance                     DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    interest_rate_annual        DECIMAL(10, 4) NOT NULL DEFAULT 0.0000,
    last_interest_applied_month VARCHAR(7),
    status                      VARCHAR(10)    NOT NULL DEFAULT 'ACTIVE',
    version                     BIGINT         NOT NULL DEFAULT 0,
    created_at                  TIMESTAMP      NOT NULL,
    updated_at                  TIMESTAMP      NOT NULL
);

ALTER TABLE savings_accounts ADD CONSTRAINT uq_account_child UNIQUE (child_user_id);
ALTER TABLE savings_accounts ADD CONSTRAINT fk_account_child FOREIGN KEY (child_user_id) REFERENCES users(id);
ALTER TABLE savings_accounts ADD CONSTRAINT fk_account_parent FOREIGN KEY (parent_user_id) REFERENCES users(id);

-- Transactions (ledger) table
CREATE TABLE transactions (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    account_id        BIGINT         NOT NULL,
    created_at        TIMESTAMP      NOT NULL,
    type              VARCHAR(20)    NOT NULL,
    amount            DECIMAL(19, 2) NOT NULL,
    balance_after     DECIMAL(19, 2) NOT NULL,
    note              VARCHAR(500),
    created_by_user_id BIGINT        NOT NULL,
    year_month        VARCHAR(7)
);

ALTER TABLE transactions ADD CONSTRAINT fk_tx_account FOREIGN KEY (account_id) REFERENCES savings_accounts(id);

-- App settings (key-value)
CREATE TABLE app_settings (
    key_name  VARCHAR(100) PRIMARY KEY,
    value     VARCHAR(500) NOT NULL
);
