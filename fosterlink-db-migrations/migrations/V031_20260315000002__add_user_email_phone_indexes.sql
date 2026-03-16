-- Performance / Security fix: DB/F-05 — Add indexes on user.email and user.phone_number
--
-- The user table has no index on email, which is used as a lookup key for login,
-- email-verification flows, and duplicate-detection checks. Without an index,
-- these queries perform full table scans and leave the column vulnerable to
-- duplicate email registrations going undetected at the DB layer.
--
-- A UNIQUE INDEX on email enforces the one-account-per-address invariant at the
-- database level (not only in application logic) and speeds up lookups.
--
-- A regular INDEX on phone_number speeds up phone-based lookups where present.
--
-- DDL review confirmed: as of V029, neither idx_user_email nor idx_user_phone_number
-- exist on the user table, so both additions are safe.

ALTER TABLE `user` ADD UNIQUE INDEX `idx_user_email` (`email`);
ALTER TABLE `user` ADD INDEX `idx_user_phone_number` (`phone_number`);
