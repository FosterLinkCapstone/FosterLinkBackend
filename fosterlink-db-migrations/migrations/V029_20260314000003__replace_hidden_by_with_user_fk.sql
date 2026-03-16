-- Agency: replace hidden_by_username with hidden_by_user_id FK + hidden_by_deletion_request flag
ALTER TABLE agency ADD COLUMN hidden_by_user_id INT NULL;
ALTER TABLE agency ADD COLUMN hidden_by_deletion_request TINYINT NOT NULL DEFAULT 0;

-- Migrate data: map existing usernames to user IDs
UPDATE agency a
    INNER JOIN `user` u ON u.username = a.hidden_by_username
SET a.hidden_by_user_id = u.id
WHERE a.hidden_by_username IS NOT NULL
  AND a.hidden_by_username != '[account-deletion-pending]';

-- Mark rows that were hidden via the account-deletion-pending flow
UPDATE agency
SET hidden_by_deletion_request = 1
WHERE hidden_by_username = '[account-deletion-pending]';

-- Add FK (after data migration so existing data is clean)
ALTER TABLE agency
    ADD CONSTRAINT fk_agency_hidden_by_user
    FOREIGN KEY (hidden_by_user_id) REFERENCES `user`(id) ON DELETE SET NULL;

-- Drop old column
ALTER TABLE agency DROP COLUMN hidden_by_username;

-- post_metadata: replace hidden_by (username string) with hidden_by_user_id FK
ALTER TABLE post_metadata ADD COLUMN hidden_by_user_id INT NULL;

-- Migrate data: map existing usernames to user IDs
UPDATE post_metadata pm
    INNER JOIN `user` u ON u.username = pm.hidden_by
SET pm.hidden_by_user_id = u.id
WHERE pm.hidden_by IS NOT NULL;

-- Add FK
ALTER TABLE post_metadata
    ADD CONSTRAINT fk_post_metadata_hidden_by_user
    FOREIGN KEY (hidden_by_user_id) REFERENCES `user`(id) ON DELETE SET NULL;

-- Drop old column
ALTER TABLE post_metadata DROP COLUMN hidden_by;


