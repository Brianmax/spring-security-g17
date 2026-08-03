--liquibase formatted sql

--changeset banking-api:001-initial-schema
CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(320) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_app_users_email UNIQUE (email),
    CONSTRAINT ck_app_users_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE bank_accounts (
    id UUID PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    owner_id UUID NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_bank_accounts_account_number UNIQUE (account_number),
    CONSTRAINT fk_bank_accounts_owner FOREIGN KEY (owner_id) REFERENCES app_users (id),
    CONSTRAINT ck_bank_accounts_type CHECK (account_type IN ('CHECKING', 'SAVINGS')),
    CONSTRAINT ck_bank_accounts_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    CONSTRAINT ck_bank_accounts_currency CHECK (currency IN ('USD', 'EUR', 'PEN')),
    CONSTRAINT ck_bank_accounts_balance CHECK (balance >= 0)
);

CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    source_account_id UUID NOT NULL,
    destination_account_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    reference VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    failure_reason VARCHAR(255),
    CONSTRAINT uk_transfers_reference UNIQUE (reference),
    CONSTRAINT uk_transfers_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_transfers_source FOREIGN KEY (source_account_id) REFERENCES bank_accounts (id),
    CONSTRAINT fk_transfers_destination FOREIGN KEY (destination_account_id) REFERENCES bank_accounts (id),
    CONSTRAINT ck_transfers_different_accounts CHECK (source_account_id <> destination_account_id),
    CONSTRAINT ck_transfers_amount CHECK (amount > 0),
    CONSTRAINT ck_transfers_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'))
);

CREATE TABLE account_transactions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    balance_after_transaction NUMERIC(19, 4) NOT NULL,
    reference VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_account_transactions_account FOREIGN KEY (account_id) REFERENCES bank_accounts (id),
    CONSTRAINT ck_account_transactions_type CHECK (
        transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER_IN', 'TRANSFER_OUT')
    ),
    CONSTRAINT ck_account_transactions_amount CHECK (amount > 0),
    CONSTRAINT ck_account_transactions_balance CHECK (balance_after_transaction >= 0)
);

CREATE INDEX idx_app_users_email ON app_users (email);
CREATE INDEX idx_bank_accounts_owner ON bank_accounts (owner_id);
CREATE INDEX idx_bank_accounts_account_number ON bank_accounts (account_number);
CREATE INDEX idx_account_transactions_account_created ON account_transactions (account_id, created_at DESC);
CREATE INDEX idx_transfers_source_account ON transfers (source_account_id);
CREATE INDEX idx_transfers_destination_account ON transfers (destination_account_id);
CREATE INDEX idx_transfers_idempotency_key ON transfers (idempotency_key);

--rollback DROP TABLE account_transactions; DROP TABLE transfers; DROP TABLE bank_accounts; DROP TABLE app_users;
