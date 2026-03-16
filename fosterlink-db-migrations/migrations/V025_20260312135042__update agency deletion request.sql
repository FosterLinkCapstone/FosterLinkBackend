-- MySQL Workbench Synchronization
-- Generated: 2026-03-12 13:50
-- Model: New Model
-- Version: 1.0
-- Project: Name of the project
-- Author: ds029

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

ALTER TABLE `fosterlink_dev`.`agency_deletion_request` 
DROP FOREIGN KEY `agency_deletion_request_ibfk_1`,
DROP FOREIGN KEY `agency_deletion_request_ibfk_2`;

ALTER TABLE `fosterlink_dev`.`agency_deletion_request` 
ADD COLUMN `reviewed_at` DATETIME NULL DEFAULT NULL AFTER `requested_by`,
ADD COLUMN `auto_approved` TINYINT(1) NOT NULL DEFAULT 0 AFTER `reviewed_at`,
ADD COLUMN `auto_approve_by` DATETIME NOT NULL AFTER `auto_approved`,
ADD COLUMN `delay_note` TEXT NULL DEFAULT NULL COMMENT 'Null if not delayed' AFTER `auto_approve_by`,
ADD COLUMN `reviewed_by` INT(11) NULL DEFAULT NULL COMMENT 'Null if unreviewed' AFTER `delay_note`,
CHANGE COLUMN `approved` `approved` TINYINT(1) NOT NULL DEFAULT 0 ,
CHANGE COLUMN `created_at` `created_at` DATETIME NOT NULL ,
CHANGE COLUMN `agency` `agency` INT(11) NULL DEFAULT NULL COMMENT 'Should be null if approved (agency record doesn\'t exist anymore)' ,
CHANGE COLUMN `requested_by` `requested_by` INT(11) NOT NULL ,
ADD INDEX `fk_agency_deletion_request_user1_idx` (`reviewed_by` ASC);
;

ALTER TABLE `fosterlink_dev`.`agency_deletion_request` 
ADD CONSTRAINT `agency_deletion_request_ibfk_1`
  FOREIGN KEY (`agency`)
  REFERENCES `fosterlink_dev`.`agency` (`id`),
ADD CONSTRAINT `agency_deletion_request_ibfk_2`
  FOREIGN KEY (`requested_by`)
  REFERENCES `fosterlink_dev`.`user` (`id`),
ADD CONSTRAINT `fk_agency_deletion_request_user1`
  FOREIGN KEY (`reviewed_by`)
  REFERENCES `fosterlink_dev`.`user` (`id`)
  ON DELETE NO ACTION
  ON UPDATE NO ACTION;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
