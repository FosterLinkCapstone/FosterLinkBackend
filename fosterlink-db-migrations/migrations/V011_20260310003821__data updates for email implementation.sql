-- MySQL Workbench Synchronization
-- Generated: 2026-03-10 00:37
-- Model: New Model
-- Version: 1.0
-- Project: Name of the project
-- Author: ds029

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

ALTER TABLE `fosterlink_dev`.`user` 
ADD COLUMN `unsubscribe_all` TINYINT(1) NOT NULL DEFAULT 0 AFTER `restricted_until`,
ADD COLUMN `usercol` VARCHAR(45) NULL DEFAULT NULL AFTER `unsubscribe_all`;

CREATE TABLE IF NOT EXISTS `fosterlink_dev`.`dont_send_email` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `email_type_id` INT(11) NOT NULL,
  `user_id` INT(11) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_dont_send_email_email_type1_idx` (`email_type_id` ASC),
  INDEX `fk_dont_send_email_user1_idx` (`user_id` ASC),
  CONSTRAINT `fk_dont_send_email_email_type1`
    FOREIGN KEY (`email_type_id`)
    REFERENCES `fosterlink_dev`.`email_type` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_dont_send_email_user1`
    FOREIGN KEY (`user_id`)
    REFERENCES `fosterlink_dev`.`user` (`id`)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;

CREATE TABLE IF NOT EXISTS `fosterlink_dev`.`email_type` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(45) NOT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
