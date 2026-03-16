-- Migrate user.unsubscribe_token to hashed storage
-- Resolves DB/F-02

-- Add the new hash column (nullable to allow population before enforcing the unique index)
ALTER TABLE `user` ADD COLUMN unsubscribe_token_hash VARCHAR(64) NULL;

-- Populate hash for all rows that have a non-null token using MySQL's SHA2 (SHA-256)
UPDATE `user` SET unsubscribe_token_hash = SHA2(unsubscribe_token, 256)
  WHERE unsubscribe_token IS NOT NULL;

-- Add unique index (allows NULLs for users who have never been emailed)
ALTER TABLE `user` ADD UNIQUE INDEX idx_user_unsubscribe_token_hash (unsubscribe_token_hash);

-- Drop the plaintext token column
ALTER TABLE `user` DROP COLUMN unsubscribe_token;
