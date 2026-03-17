ALTER TABLE `user` ADD COLUMN `unsubscribe_token` VARCHAR(100) NULL COMMENT 'Raw unsubscribe token embedded in emails; null until first email is sent';
