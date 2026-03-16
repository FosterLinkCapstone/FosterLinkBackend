-- Security fix: DB/F-11 — Add unsubscribe_token_issued_at for rotation tracking
--
-- The unsubscribe_token column (added in V020) stores the raw token embedded in
-- outbound emails. Without a timestamp recording when the token was issued, the
-- application cannot enforce maximum token age or detect stale tokens that should
-- be rotated. This column enables token rotation policies (e.g., reissue on each
-- email send, expire tokens older than N days).
--
-- Existing rows with a non-null unsubscribe_token are back-filled with NOW() so
-- that they are not immediately treated as expired upon deployment.

ALTER TABLE `user` ADD COLUMN `unsubscribe_token_issued_at` DATETIME NULL AFTER `unsubscribe_token`;
UPDATE `user` SET `unsubscribe_token_issued_at` = NOW() WHERE `unsubscribe_token` IS NOT NULL;
