ALTER TABLE transfers
    ADD COLUMN reviewed_at TIMESTAMPTZ;

CREATE INDEX idx_transfers_status_created_at ON transfers(status, created_at);
CREATE INDEX idx_transfers_amount ON transfers(amount);
CREATE INDEX idx_transfers_reviewed_at ON transfers(reviewed_at);
