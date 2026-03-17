-- Schema sync: DB/F-15 — Restore AUTO_INCREMENT on account_deletion_request.id and
--               add missing idx_post_metadata_deleted_at
--
-- Two structural divergences were found between the dev schema and the Flyway-managed
-- schema by diffing MySQL Workbench forward-engineering exports.
--
-- (1) account_deletion_request.id is missing AUTO_INCREMENT in this environment.
--     The column is the table's primary key and must auto-generate IDs on insert.
--     This was lost during a prior schema generation step that is not applied on dev.
--
-- (2) post_metadata is missing the idx_post_metadata_deleted_at index.
--     This index supports efficient filtering/cleanup queries on soft-deleted posts
--     and exists on dev. The omission causes full-table scans on deleted_at lookups.

ALTER TABLE `account_deletion_request` MODIFY COLUMN `id` INT NOT NULL AUTO_INCREMENT;

ALTER TABLE `post_metadata` ADD INDEX `idx_post_metadata_deleted_at` (`deleted_at`);
