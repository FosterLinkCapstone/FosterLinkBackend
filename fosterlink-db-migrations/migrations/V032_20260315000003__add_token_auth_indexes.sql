-- Performance / Security fix: DB/F-09, DB/F-06 — Add indexes on token_auth
--
-- DB/F-09: The token_auth table has no index on expires_at. Scheduled purge jobs
-- and expiry-check queries scan the full table, which degrades as the table grows.
-- An index on expires_at makes range-based expiry queries and deletions efficient.
--
-- DB/F-06: The token column in token_auth has no uniqueness constraint at the
-- database level, relying solely on application-layer generation to avoid
-- collisions. A UNIQUE KEY enforces this invariant in the database and prevents
-- duplicate token rows from being inserted concurrently.
--
-- NOTE: The uq_token_auth_token unique key added here is a temporary integrity
-- measure. It will be superseded by a forthcoming token-hashing migration
-- (expected V035-V036) that replaces the plain-text token column with a
-- token_hash column. That migration will drop this key and introduce a unique
-- index on token_hash instead.

ALTER TABLE `token_auth` ADD INDEX `idx_token_auth_expires_at` (`expires_at`);
ALTER TABLE `token_auth` ADD UNIQUE KEY `uq_token_auth_token` (`token`);
