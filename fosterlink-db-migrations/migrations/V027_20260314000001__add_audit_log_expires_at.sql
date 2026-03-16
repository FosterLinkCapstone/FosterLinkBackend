-- Adds a stored generated column so every audit_log row automatically carries its
-- expiry date (730 days / 2 years after creation), enabling fast scheduler-driven cleanup.
ALTER TABLE audit_log
    ADD COLUMN expires_at DATETIME
        GENERATED ALWAYS AS (DATE_ADD(created_at, INTERVAL 730 DAY)) STORED;

ALTER TABLE audit_log
    ADD INDEX idx_audit_log_expires_at (expires_at);
