-- Security fix: DB/F-01 (Critical) — Remove hardcoded default password hash
--
-- V001 defined user.password with a DEFAULT value of the SHA-256 hash of the
-- string 'password' (5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8).
-- Any INSERT that omitted the password column would silently assign this known,
-- publicly-documented hash, leaving the account immediately crackable by any
-- attacker who looks at the schema or source-controlled migration history.
--
-- This migration removes the DEFAULT clause so that omitting a password on INSERT
-- becomes a hard error (NOT NULL without DEFAULT), forcing callers to always
-- supply an explicit, properly hashed credential.

ALTER TABLE `user` MODIFY COLUMN `password` VARCHAR(255) NOT NULL;
