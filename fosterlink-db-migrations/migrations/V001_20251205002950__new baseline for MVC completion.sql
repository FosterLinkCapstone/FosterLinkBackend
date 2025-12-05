-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema mydb
-- -----------------------------------------------------
-- -----------------------------------------------------
-- Schema fosterlink_dev
-- -----------------------------------------------------

-- -----------------------------------------------------
-- Schema fosterlink_dev
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `fosterlink_dev` DEFAULT CHARACTER SET utf8 ;
USE `fosterlink_dev` ;

-- -----------------------------------------------------
-- Table `fosterlink_dev`.`user`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `fosterlink_dev`.`user` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `first_name` VARCHAR(255) NULL DEFAULT NULL,
  `last_name` VARCHAR(255) NULL DEFAULT NULL,
  `username` VARCHAR(45) NOT NULL,
  `email` VARCHAR(255) NULL DEFAULT NULL,
  `phone_number` VARCHAR(255) NULL DEFAULT NULL,
  `profile_picture_url` VARCHAR(255) NULL DEFAULT NULL,
  `id_verified` BIT(1) NOT NULL,
  `verified_foster` BIT(1) NOT NULL,
  `verified_agency_rep` BIT(1) NOT NULL,
  `administrator` BIT(1) NOT NULL,
  `faq_author` BIT(1) NOT NULL,
  `email_verified` BIT(1) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NULL DEFAULT NULL,
  `password` VARCHAR(255) NOT NULL DEFAULT '5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8',
  PRIMARY KEY (`id`),
  INDEX `idx_user_username` (`username` ASC),
  INDEX `idx_user_verified_foster` (`verified_foster` ASC))
ENGINE = InnoDB
AUTO_INCREMENT = 17
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `fosterlink_dev`.`location`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `fosterlink_dev`.`location` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `city` VARCHAR(255) NULL DEFAULT NULL,
  `state` VARCHAR(255) NULL DEFAULT NULL,
  `zip_code` INT(11) NOT NULL,
  `addr_line1` VARCHAR(255) NULL DEFAULT NULL,
  `addr_line2` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 8
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `fosterlink_dev`.`agency`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `fosterlink_dev`.`agency` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NULL DEFAULT NULL,
  `mission_statement` TEXT NOT NULL,
  `address` INT(11) NULL DEFAULT NULL,
  `agent` INT(11) NOT NULL,
  `website_url` VARCHAR(100) NOT NULL,
  `approved` TINYINT(1) NULL DEFAULT NULL,
  `approved_by_id` INT(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_agency_location1_idx` (`address` ASC),
  INDEX `address` (`address` ASC),
  INDEX `agent` (`agent` ASC),
  INDEX `approved_by_id` (`approved_by_id` ASC),
  CONSTRAINT `agency_ibfk_1`
    FOREIGN KEY (`agent`)
    REFERENCES `fosterlink_dev`.`user` (`id`),
  CONSTRAINT `agency_ibfk_2`
    FOREIGN KEY (`approved_by_id`)
    REFERENCES `fosterlink_dev`.`user` (`id`),
  CONSTRAINT `fk_agency_location1`
    FOREIGN KEY (`address`)
    REFERENCES `fosterlink_dev`.`location` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 8
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `fosterlink_dev`.`agency_deletion_request`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `fosterlink_dev`.`agency_deletion_request` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `approved` TINYINT(1) NULL DEFAULT NULL,
  `created_at` DATETIME NULL DEFAULT NULL,
  `agency` INT(11) NULL DEFAULT NULL,
  `requested_by` INT(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `agency` (`agency` ASC),
  INDEX `requested_by` (`requested_by` ASC),
  CONSTRAINT `agency_deletion_request_ibfk_1`
    FOREIGN KEY (`agency`)
    REFERENCES `fosterlink_dev`.`agency` (`id`),
  CONSTRAINT `agency_deletion_request_ibfk_2`
    FOREIGN KEY (`requested_by`)
    REFERENCES `fosterlink_dev`.`agent` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `fosterlink_dev`.`faq`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `fosterlink_dev`.`faq` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(255) NULL DEFAULT NULL,
  `content` TEXT NOT NULL,
  `summary` VARCHAR(255) NULL DEFAULT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NULL DEFAULT NULL,
  `author` INT(11) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `author` (`author` ASC),
  CONSTRAINT `fk_FAQ_user1`
    FOREIGN KEY (`author`)
    REFERENCES `fosterlink_dev`.`user` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 25
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `fosterlink_dev`.`faq_approval`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `fosterlink_dev`.`faq_approval` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `faq_id` INT(11) NOT NULL,
  `approved` TINYINT(1) NOT NULL,
  `approved_by_id` INT(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `faq_id` (`faq_id` ASC),
  INDEX `approved_by_id` (`approved_by_id` ASC),
  CONSTRAINT `faq_approval_ibfk_1`
    FOREIGN KEY (`faq_id`)
    REFERENCES `fosterlink_dev`.`faq` (`id`),
  CONSTRAINT `faq_approval_ibfk_2`
    FOREIGN KEY (`approved_by_id`)
    REFERENCES `fosterlink_dev`.`user` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 23
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `fosterlink_dev`.`faq_request`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `fosterlink_dev`.`faq_request` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `requested_by` INT(11) NOT NULL COMMENT 'fk user.id',
  `suggested_topic` TEXT NOT NULL COMMENT 'effectively title',
  `created_at` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_requested_by_id` (`requested_by` ASC),
  CONSTRAINT `fk_requested_by_id`
    FOREIGN KEY (`requested_by`)
    REFERENCES `fosterlink_dev`.`user` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 13
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `fosterlink_dev`.`post_metadata`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `fosterlink_dev`.`post_metadata` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `hidden` BIT(1) NOT NULL,
  `user_deleted` BIT(1) NOT NULL,
  `locked` BIT(1) NOT NULL,
  `verified` BIT(1) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_post_metadata_hidden` (`hidden` ASC),
  INDEX `idx_post_metadata_user_deleted` (`user_deleted` ASC))
ENGINE = InnoDB
AUTO_INCREMENT = 27
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `fosterlink_dev`.`thread`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `fosterlink_dev`.`thread` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(255) NULL DEFAULT NULL,
  `content` TEXT NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NULL DEFAULT NULL,
  `metadata` INT(11) NOT NULL,
  `posted_by` INT(11) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_thread_post_metadata_idx` (`metadata` ASC),
  INDEX `fk_thread_User1_idx` (`posted_by` ASC),
  INDEX `metadata` (`metadata` ASC),
  INDEX `posted_by` (`posted_by` ASC),
  INDEX `idx_thread_title` (`title` ASC),
  CONSTRAINT `fk_thread_User1`
    FOREIGN KEY (`posted_by`)
    REFERENCES `fosterlink_dev`.`user` (`id`),
  CONSTRAINT `fk_thread_post_metadata`
    FOREIGN KEY (`metadata`)
    REFERENCES `fosterlink_dev`.`post_metadata` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 37
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `fosterlink_dev`.`thread_like`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `fosterlink_dev`.`thread_like` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `thread` INT(11) NOT NULL,
  `user` INT(11) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_thread_like_thread1_idx` (`thread` ASC),
  INDEX `fk_thread_like_User1_idx` (`user` ASC),
  INDEX `thread` (`thread` ASC),
  INDEX `user` (`user` ASC),
  CONSTRAINT `fk_thread_like_User1`
    FOREIGN KEY (`user`)
    REFERENCES `fosterlink_dev`.`user` (`id`),
  CONSTRAINT `fk_thread_like_thread1`
    FOREIGN KEY (`thread`)
    REFERENCES `fosterlink_dev`.`thread` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 105
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `fosterlink_dev`.`thread_reply`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `fosterlink_dev`.`thread_reply` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `content` TEXT NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NULL DEFAULT NULL,
  `metadata` INT(11) NOT NULL,
  `posted_by` INT(11) NOT NULL,
  `thread_id` INT(11) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_thread_reply_post_metadata1_idx` (`metadata` ASC),
  INDEX `fk_thread_reply_User1_idx` (`posted_by` ASC),
  INDEX `metadata` (`metadata` ASC),
  INDEX `posted_by` (`posted_by` ASC),
  INDEX `fk_thread_reply_thread_id` (`thread_id` ASC),
  CONSTRAINT `fk_thread_reply_User1`
    FOREIGN KEY (`posted_by`)
    REFERENCES `fosterlink_dev`.`user` (`id`),
  CONSTRAINT `fk_thread_reply_post_metadata1`
    FOREIGN KEY (`metadata`)
    REFERENCES `fosterlink_dev`.`post_metadata` (`id`),
  CONSTRAINT `fk_thread_reply_thread_id`
    FOREIGN KEY (`thread_id`)
    REFERENCES `fosterlink_dev`.`thread` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 9
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `fosterlink_dev`.`thread_reply_like`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `fosterlink_dev`.`thread_reply_like` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `thread` INT(11) NOT NULL,
  `user` INT(11) NOT NULL,
  `thread_id` INT(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  INDEX `fk_thread_reply_like_thread_reply1_idx` (`thread` ASC),
  INDEX `fk_thread_reply_like_User1_idx` (`user` ASC),
  INDEX `thread` (`thread` ASC),
  INDEX `user` (`user` ASC),
  INDEX `FKhxu4hwmeh5ywg2gc1pt9kftg4` (`thread_id` ASC),
  CONSTRAINT `FKhxu4hwmeh5ywg2gc1pt9kftg4`
    FOREIGN KEY (`thread_id`)
    REFERENCES `fosterlink_dev`.`thread_reply` (`id`),
  CONSTRAINT `fk_thread_reply_like_User1`
    FOREIGN KEY (`user`)
    REFERENCES `fosterlink_dev`.`user` (`id`),
  CONSTRAINT `fk_thread_reply_like_thread_reply1`
    FOREIGN KEY (`thread`)
    REFERENCES `fosterlink_dev`.`thread_reply` (`id`))
ENGINE = InnoDB
AUTO_INCREMENT = 10
DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `fosterlink_dev`.`thread_tag`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `fosterlink_dev`.`thread_tag` (
  `id` INT(11) NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NULL DEFAULT NULL,
  `thread` INT(11) NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `thread` (`thread` ASC),
  CONSTRAINT `fk_thread_tag_thread1`
    FOREIGN KEY (`thread`)
    REFERENCES `fosterlink_dev`.`thread` (`id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8;

USE `fosterlink_dev` ;

-- -----------------------------------------------------
-- procedure describe_and_count
-- -----------------------------------------------------

DELIMITER $$
USE `fosterlink_dev`$$
CREATE DEFINER=`developer`@`%` PROCEDURE `describe_and_count`()
BEGIN
    SELECT COUNT(*) AS user_count FROM fosterlink_dev.`user`;
    DESCRIBE fosterlink_dev.`user`;
END$$

DELIMITER ;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
