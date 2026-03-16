-- Adds a flag to agency that lets the creating agent opt in to showing their
-- email and phone number on the public agency listing page.
-- Defaults to 0 (opt-out) so no existing agency unexpectedly exposes PII.
ALTER TABLE agency
    ADD COLUMN show_contact_info TINYINT(1) NOT NULL DEFAULT 0
        COMMENT 'Agent opted in to showing their email and phone number publicly (1 = show, 0 = hide)';
