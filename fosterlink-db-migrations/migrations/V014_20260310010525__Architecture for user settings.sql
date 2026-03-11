ALTER TABLE `email_type` ADD `can_disable` TINYINT(1) NOT NULL DEFAULT '1' AFTER `name`;
ALTER TABLE `email_type` ADD `ui_name` VARCHAR(100) NULL AFTER `can_disable`;