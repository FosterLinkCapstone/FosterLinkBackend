-- Migrate token_auth.token to hashed storage (security: plaintext tokens are a breach risk)
-- Resolves DB/F-03

-- Add the new hash column (nullable to allow population before enforcing NOT NULL)
ALTER TABLE token_auth ADD COLUMN token_hash VARCHAR(64) NULL;

-- Populate hash column by copying the existing value: TokenAuthService already stored
-- SHA-256 hashes in the token column, so we copy directly rather than re-hashing.
-- Re-applying SHA2 here would produce a double-hash and invalidate all outstanding tokens.
UPDATE token_auth SET token_hash = token;

-- Enforce NOT NULL now that all rows are populated
ALTER TABLE token_auth MODIFY COLUMN token_hash VARCHAR(64) NOT NULL;

-- Add unique index on the hash column
ALTER TABLE token_auth ADD UNIQUE KEY uq_token_auth_token_hash (token_hash);

-- Drop the plaintext token column; MySQL automatically drops any indexes associated with it
ALTER TABLE token_auth DROP COLUMN token;
