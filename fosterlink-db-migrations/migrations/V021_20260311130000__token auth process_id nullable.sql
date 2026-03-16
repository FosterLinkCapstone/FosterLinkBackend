ALTER TABLE `token_auth` MODIFY COLUMN `process_id` VARCHAR(100) NULL COMMENT 'When null, token has no sibling group (e.g. persistent unsubscribe token)';
