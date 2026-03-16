-- MySQL Workbench Synchronization
-- Generated: 2026-03-12 13:50
-- Model: New Model
-- Version: 1.0
-- Project: Name of the project
-- Author: ds029

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- Drop foreign keys if they exist (MySQL 5.7 compatible conditional drop)
SET @fk1_exists = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'agency_deletion_request'
    AND CONSTRAINT_NAME = 'agency_deletion_request_ibfk_1'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @sql1 = IF(@fk1_exists > 0,
    'ALTER TABLE `agency_deletion_request` DROP FOREIGN KEY `agency_deletion_request_ibfk_1`',
    'SELECT 1'
);
PREPARE stmt FROM @sql1;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @fk2_exists = (
    SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'agency_deletion_request'
    AND CONSTRAINT_NAME = 'agency_deletion_request_ibfk_2'
    AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @sql2 = IF(@fk2_exists > 0,
    'ALTER TABLE `agency_deletion_request` DROP FOREIGN KEY `agency_deletion_request_ibfk_2`',
    'SELECT 1'
);
PREPARE stmt FROM @sql2;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Remove invalid rows that would violate the NOT NULL constraints being added below.
-- A deletion request with no requester or no timestamp is not a valid record.
DELETE FROM `agency_deletion_request` WHERE `requested_by` IS NULL OR `created_at` IS NULL;

-- Coerce any remaining NULL approved values to 0 (not yet approved) before making it NOT NULL.
UPDATE `agency_deletion_request` SET `approved` = 0 WHERE `approved` IS NULL;

-- Add new columns; auto_approve_by is nullable here so existing rows can be backfilled below
ALTER TABLE `agency_deletion_request` 
ADD COLUMN `reviewed_at` DATETIME NULL DEFAULT NULL AFTER `requested_by`,
ADD COLUMN `auto_approved` TINYINT(1) NOT NULL DEFAULT 0 AFTER `reviewed_at`,
ADD COLUMN `auto_approve_by` DATETIME NULL DEFAULT NULL AFTER `auto_approved`,
ADD COLUMN `delay_note` TEXT NULL DEFAULT NULL COMMENT 'Null if not delayed' AFTER `auto_approve_by`,
ADD COLUMN `reviewed_by` INT(11) NULL DEFAULT NULL COMMENT 'Null if unreviewed' AFTER `delay_note`,
CHANGE COLUMN `approved` `approved` TINYINT(1) NOT NULL DEFAULT 0 ,
CHANGE COLUMN `created_at` `created_at` DATETIME NOT NULL ,
CHANGE COLUMN `agency` `agency` INT(11) NULL DEFAULT NULL COMMENT 'Should be null if approved (agency record doesn\'t exist anymore)' ,
CHANGE COLUMN `requested_by` `requested_by` INT(11) NOT NULL ,
ADD INDEX `fk_agency_deletion_request_user1_idx` (`reviewed_by` ASC);

-- Backfill auto_approve_by for any existing rows to 30 days after their request was created
UPDATE `agency_deletion_request`
SET `auto_approve_by` = DATE_ADD(`created_at`, INTERVAL 30 DAY)
WHERE `auto_approve_by` IS NULL;

-- Now that all rows have a value, enforce NOT NULL
ALTER TABLE `agency_deletion_request`
CHANGE COLUMN `auto_approve_by` `auto_approve_by` DATETIME NOT NULL;

ALTER TABLE `agency_deletion_request` 
ADD CONSTRAINT `agency_deletion_request_ibfk_1`
  FOREIGN KEY (`agency`)
  REFERENCES `agency` (`id`),
ADD CONSTRAINT `agency_deletion_request_ibfk_2`
  FOREIGN KEY (`requested_by`)
  REFERENCES `user` (`id`),
ADD CONSTRAINT `fk_agency_deletion_request_user1`
  FOREIGN KEY (`reviewed_by`)
  REFERENCES `user` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
