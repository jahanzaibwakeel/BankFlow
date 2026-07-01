CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(160) NOT NULL UNIQUE,
    full_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(40) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT ck_user_roles_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
);

CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    account_number VARCHAR(32) NOT NULL UNIQUE,
    type VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    balance NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_accounts_type CHECK (type IN ('CHECKING', 'SAVINGS')),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED')),
    CONSTRAINT ck_accounts_balance_non_negative CHECK (balance >= 0)
);

CREATE TABLE transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_account_id UUID REFERENCES accounts(id),
    destination_account_id UUID REFERENCES accounts(id),
    amount NUMERIC(19,2) NOT NULL,
    description VARCHAR(255),
    status VARCHAR(40) NOT NULL,
    reviewed_by VARCHAR(120),
    review_note VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_transfers_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_transfers_status CHECK (status IN ('COMPLETED', 'REJECTED', 'PENDING_REVIEW', 'REVERSED')),
    CONSTRAINT ck_transfers_has_account CHECK (source_account_id IS NOT NULL OR destination_account_id IS NOT NULL),
    CONSTRAINT ck_transfers_distinct_accounts CHECK (source_account_id IS NULL OR destination_account_id IS NULL OR source_account_id <> destination_account_id)
);

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES accounts(id),
    transfer_id UUID REFERENCES transfers(id),
    entry_type VARCHAR(20) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    balance_after NUMERIC(19,2) NOT NULL,
    reference VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_ledger_entry_type CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_ledger_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_ledger_balance_non_negative CHECK (balance_after >= 0)
);

CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(160) NOT NULL,
    idempotency_key VARCHAR(160) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    operation VARCHAR(64) NOT NULL,
    resource_id VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_idempotency_user_key_operation UNIQUE (user_id, idempotency_key, operation)
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id VARCHAR(80),
    action VARCHAR(80) NOT NULL,
    resource_type VARCHAR(80) NOT NULL,
    resource_id VARCHAR(80),
    details VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_accounts_created_at ON accounts(created_at);
CREATE INDEX idx_transfers_source_account_id ON transfers(source_account_id);
CREATE INDEX idx_transfers_destination_account_id ON transfers(destination_account_id);
CREATE INDEX idx_transfers_created_at ON transfers(created_at);
CREATE INDEX idx_ledger_entries_account_id ON ledger_entries(account_id);
CREATE INDEX idx_ledger_entries_transfer_id ON ledger_entries(transfer_id);
CREATE INDEX idx_ledger_entries_created_at ON ledger_entries(created_at);
CREATE INDEX idx_idempotency_created_at ON idempotency_keys(created_at);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_actor_user_id ON audit_logs(actor_user_id);
