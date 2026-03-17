-- MySQL Workbench Forward Engineering

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Table `user`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
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
                                      `password` VARCHAR(255) NOT NULL,
                                      `account_deleted` TINYINT(4) NOT NULL DEFAULT '0',
                                      `banned_at` DATETIME NULL DEFAULT NULL,
                                      `restricted_at` DATETIME NULL DEFAULT NULL,
                                      `restricted_until` DATETIME NULL DEFAULT NULL,
                                      `unsubscribe_all` TINYINT(1) NOT NULL DEFAULT '0',
                                      `unsubscribe_token` VARCHAR(100) NULL DEFAULT NULL COMMENT 'Raw unsubscribe token embedded in emails; null until first email is sent',
                                      `auth_token_version` INT(11) NOT NULL DEFAULT '0',
                                      PRIMARY KEY (`id`),
                                      INDEX `idx_user_username` (`username` ASC),
                                      INDEX `idx_user_verified_foster` (`verified_foster` ASC))
    ENGINE = InnoDB
    AUTO_INCREMENT = 21
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `account_deletion_request`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `account_deletion_request` (
                                                          `id` INT(11) NOT NULL AUTO_INCREMENT,
                                                          `requested_by_email_hash` VARCHAR(255) NOT NULL,
                                                          `requested_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                          `reviewed_at` DATETIME NULL DEFAULT NULL,
                                                          `auto_approved` TINYINT(4) NOT NULL DEFAULT '0',
                                                          `auto_approve_by` DATETIME NOT NULL,
                                                          `approved` TINYINT(4) NOT NULL DEFAULT '0',
                                                          `delay_note` VARCHAR(1500) NULL DEFAULT NULL,
                                                          `requested_by` INT(11) NOT NULL,
                                                          `reviewed_by` INT(11) NULL DEFAULT NULL,
                                                          `clear_account` TINYINT(1) NOT NULL DEFAULT '0',
                                                          PRIMARY KEY (`id`),
                                                          INDEX `fk_account_deletion_request_user1_idx` (`requested_by` ASC),
                                                          INDEX `fk_account_deletion_request_user2_idx` (`reviewed_by` ASC),
                                                          CONSTRAINT `fk_account_deletion_request_user1`
                                                              FOREIGN KEY (`requested_by`)
                                                                  REFERENCES `user` (`id`)
                                                                  ON DELETE NO ACTION
                                                                  ON UPDATE NO ACTION,
                                                          CONSTRAINT `fk_account_deletion_request_user2`
                                                              FOREIGN KEY (`reviewed_by`)
                                                                  REFERENCES `user` (`id`)
                                                                  ON DELETE NO ACTION
                                                                  ON UPDATE NO ACTION)
    ENGINE = InnoDB
    AUTO_INCREMENT = 10
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `location`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `location` (
                                          `id` INT(11) NOT NULL AUTO_INCREMENT,
                                          `city` VARCHAR(255) NULL DEFAULT NULL,
                                          `state` VARCHAR(255) NULL DEFAULT NULL,
                                          `zip_code` INT(11) NOT NULL,
                                          `addr_line1` VARCHAR(255) NULL DEFAULT NULL,
                                          `addr_line2` VARCHAR(255) NULL DEFAULT NULL,
                                          PRIMARY KEY (`id`))
    ENGINE = InnoDB
    AUTO_INCREMENT = 6
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `agency`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `agency` (
                                        `id` INT(11) NOT NULL AUTO_INCREMENT,
                                        `name` VARCHAR(255) NULL DEFAULT NULL,
                                        `mission_statement` TEXT NOT NULL,
                                        `address` INT(11) NULL DEFAULT NULL,
                                        `agent` INT(11) NOT NULL,
                                        `website_url` VARCHAR(100) NOT NULL,
                                        `approved` TINYINT(1) NULL DEFAULT NULL,
                                        `approved_by_id` INT(11) NULL DEFAULT NULL,
                                        `hidden` TINYINT(1) NULL DEFAULT '0',
                                        `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        `updated_at` DATETIME NULL DEFAULT NULL,
                                        `hidden_by_user_id` INT(11) NULL DEFAULT NULL,
                                        `hidden_by_deletion_request` TINYINT(4) NOT NULL DEFAULT '0',
                                        PRIMARY KEY (`id`),
                                        INDEX `fk_agency_location1_idx` (`address` ASC),
                                        INDEX `address` (`address` ASC),
                                        INDEX `agent` (`agent` ASC),
                                        INDEX `approved_by_id` (`approved_by_id` ASC),
                                        INDEX `fk_agency_hidden_by_user` (`hidden_by_user_id` ASC),
                                        CONSTRAINT `agency_ibfk_1`
                                            FOREIGN KEY (`agent`)
                                                REFERENCES `user` (`id`),
                                        CONSTRAINT `agency_ibfk_2`
                                            FOREIGN KEY (`approved_by_id`)
                                                REFERENCES `user` (`id`),
                                        CONSTRAINT `fk_agency_hidden_by_user`
                                            FOREIGN KEY (`hidden_by_user_id`)
                                                REFERENCES `user` (`id`)
                                                ON DELETE SET NULL,
                                        CONSTRAINT `fk_agency_location1`
                                            FOREIGN KEY (`address`)
                                                REFERENCES `location` (`id`))
    ENGINE = InnoDB
    AUTO_INCREMENT = 6
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `agency_deletion_request`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `agency_deletion_request` (
                                                         `id` INT(11) NOT NULL AUTO_INCREMENT,
                                                         `approved` TINYINT(1) NOT NULL DEFAULT '0',
                                                         `auto_approve_by` DATETIME NOT NULL,
                                                         `created_at` DATETIME NOT NULL,
                                                         `agency` INT(11) NULL DEFAULT NULL COMMENT 'Should be null if approved (agency record doesn\'t exist anymore)',
                                                         `requested_by` INT(11) NOT NULL,
                                                         `reviewed_at` DATETIME NULL DEFAULT NULL,
                                                         `auto_approved` TINYINT(1) NOT NULL DEFAULT '0',
                                                         `delay_note` TEXT NULL DEFAULT NULL COMMENT 'Null if not delayed',
                                                         `reviewed_by` INT(11) NULL DEFAULT NULL COMMENT 'Null if unreviewed',
                                                         PRIMARY KEY (`id`),
                                                         INDEX `agency` (`agency` ASC),
                                                         INDEX `requested_by` (`requested_by` ASC),
                                                         INDEX `fk_agency_deletion_request_user1_idx` (`reviewed_by` ASC),
                                                         CONSTRAINT `agency_deletion_request_ibfk_1`
                                                             FOREIGN KEY (`agency`)
                                                                 REFERENCES `agency` (`id`),
                                                         CONSTRAINT `agency_deletion_request_ibfk_2`
                                                             FOREIGN KEY (`requested_by`)
                                                                 REFERENCES `user` (`id`),
                                                         CONSTRAINT `fk_agency_deletion_request_user1`
                                                             FOREIGN KEY (`reviewed_by`)
                                                                 REFERENCES `user` (`id`)
                                                                 ON DELETE NO ACTION
                                                                 ON UPDATE NO ACTION)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `audit_log`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `audit_log` (
                                           `id` INT(11) NOT NULL AUTO_INCREMENT,
                                           `action` VARCHAR(200) NOT NULL,
                                           `acting_user_id` INT(11) NULL DEFAULT NULL,
                                           `target_user_id` INT(11) NOT NULL,
                                           `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           `expires_at` DATETIME GENERATED ALWAYS AS ((`created_at` + interval 730 day)) STORED,
                                           PRIMARY KEY (`id`),
                                           INDEX `fk_audit_log_user1_idx` (`acting_user_id` ASC),
                                           INDEX `fk_audit_log_user2_idx` (`target_user_id` ASC),
                                           INDEX `idx_audit_log_expires_at` (`expires_at` ASC),
                                           CONSTRAINT `fk_audit_log_user1`
                                               FOREIGN KEY (`acting_user_id`)
                                                   REFERENCES `user` (`id`)
                                                   ON DELETE NO ACTION
                                                   ON UPDATE NO ACTION,
                                           CONSTRAINT `fk_audit_log_user2`
                                               FOREIGN KEY (`target_user_id`)
                                                   REFERENCES `user` (`id`)
                                                   ON DELETE NO ACTION
                                                   ON UPDATE NO ACTION)
    ENGINE = InnoDB
    AUTO_INCREMENT = 32
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `consent_record`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `consent_record` (
                                                `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
                                                `user_id` INT(11) NOT NULL,
                                                `consent_type` VARCHAR(50) NOT NULL,
                                                `granted` TINYINT(1) NOT NULL,
                                                `timestamp` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                `policy_version` VARCHAR(20) NULL DEFAULT NULL,
                                                `mechanism` VARCHAR(50) NULL DEFAULT NULL,
                                                `ip_address` VARCHAR(45) NULL DEFAULT NULL,
                                                PRIMARY KEY (`id`),
                                                INDEX `idx_consent_record_user_id` (`user_id` ASC),
                                                CONSTRAINT `fk_consent_record_user`
                                                    FOREIGN KEY (`user_id`)
                                                        REFERENCES `user` (`id`)
                                                        ON DELETE CASCADE)
    ENGINE = InnoDB
    AUTO_INCREMENT = 5
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `email_type`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `email_type` (
                                            `id` INT(11) NOT NULL AUTO_INCREMENT,
                                            `name` VARCHAR(45) NOT NULL,
                                            `can_disable` TINYINT(1) NOT NULL DEFAULT '1',
                                            `ui_name` VARCHAR(100) NULL DEFAULT NULL,
                                            PRIMARY KEY (`id`))
    ENGINE = InnoDB
    AUTO_INCREMENT = 12
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `dont_send_email`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `dont_send_email` (
                                                 `id` INT(11) NOT NULL AUTO_INCREMENT,
                                                 `email_type_id` INT(11) NOT NULL,
                                                 `user_id` INT(11) NOT NULL,
                                                 PRIMARY KEY (`id`),
                                                 INDEX `fk_dont_send_email_email_type1_idx` (`email_type_id` ASC),
                                                 INDEX `fk_dont_send_email_user1_idx` (`user_id` ASC),
                                                 CONSTRAINT `fk_dont_send_email_email_type1`
                                                     FOREIGN KEY (`email_type_id`)
                                                         REFERENCES `email_type` (`id`)
                                                         ON DELETE NO ACTION
                                                         ON UPDATE NO ACTION,
                                                 CONSTRAINT `fk_dont_send_email_user1`
                                                     FOREIGN KEY (`user_id`)
                                                         REFERENCES `user` (`id`)
                                                         ON DELETE NO ACTION
                                                         ON UPDATE NO ACTION)
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `faq`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `faq` (
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
                                             REFERENCES `user` (`id`))
    ENGINE = InnoDB
    AUTO_INCREMENT = 30
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `faq_approval`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `faq_approval` (
                                              `id` INT(11) NOT NULL AUTO_INCREMENT,
                                              `faq_id` INT(11) NOT NULL,
                                              `approved` TINYINT(1) NOT NULL,
                                              `approved_by_id` INT(11) NULL DEFAULT NULL,
                                              `hidden_by` VARCHAR(50) NULL DEFAULT NULL,
                                              `hidden_by_author` TINYINT(1) NOT NULL DEFAULT '0',
                                              PRIMARY KEY (`id`),
                                              INDEX `faq_id` (`faq_id` ASC),
                                              INDEX `approved_by_id` (`approved_by_id` ASC),
                                              CONSTRAINT `faq_approval_ibfk_1`
                                                  FOREIGN KEY (`faq_id`)
                                                      REFERENCES `faq` (`id`),
                                              CONSTRAINT `faq_approval_ibfk_2`
                                                  FOREIGN KEY (`approved_by_id`)
                                                      REFERENCES `user` (`id`))
    ENGINE = InnoDB
    AUTO_INCREMENT = 32
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `faq_request`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `faq_request` (
                                             `id` INT(11) NOT NULL AUTO_INCREMENT,
                                             `requested_by` INT(11) NOT NULL COMMENT 'fk user.id',
                                             `suggested_topic` TEXT NOT NULL COMMENT 'effectively title',
                                             `created_at` DATETIME NOT NULL,
                                             PRIMARY KEY (`id`),
                                             INDEX `fk_requested_by_id` (`requested_by` ASC),
                                             CONSTRAINT `fk_requested_by_id`
                                                 FOREIGN KEY (`requested_by`)
                                                     REFERENCES `user` (`id`))
    ENGINE = InnoDB
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `mailing_list`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mailing_list` (
                                              `id` INT(11) NOT NULL AUTO_INCREMENT,
                                              `name` VARCHAR(255) NOT NULL,
                                              PRIMARY KEY (`id`))
    ENGINE = InnoDB
    AUTO_INCREMENT = 3
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `mailing_list_member`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `mailing_list_member` (
                                                     `id` INT(11) NOT NULL AUTO_INCREMENT,
                                                     `user_id` INT(11) NOT NULL,
                                                     `mailing_list_id` INT(11) NOT NULL,
                                                     PRIMARY KEY (`id`),
                                                     UNIQUE INDEX `uq_mailing_list_member_user_list` (`user_id` ASC, `mailing_list_id` ASC),
                                                     INDEX `idx_mailing_list_member_user_id` (`user_id` ASC),
                                                     INDEX `idx_mailing_list_member_mailing_list_id` (`mailing_list_id` ASC),
                                                     CONSTRAINT `fk_mailing_list_member_mailing_list`
                                                         FOREIGN KEY (`mailing_list_id`)
                                                             REFERENCES `mailing_list` (`id`)
                                                             ON DELETE CASCADE,
                                                     CONSTRAINT `fk_mailing_list_member_user`
                                                         FOREIGN KEY (`user_id`)
                                                             REFERENCES `user` (`id`)
                                                             ON DELETE CASCADE)
    ENGINE = InnoDB
    AUTO_INCREMENT = 3
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `post_metadata`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `post_metadata` (
                                               `id` INT(11) NOT NULL AUTO_INCREMENT,
                                               `hidden` BIT(1) NOT NULL,
                                               `user_deleted` BIT(1) NOT NULL,
                                               `locked` BIT(1) NOT NULL,
                                               `verified` BIT(1) NOT NULL,
                                               `deleted_at` DATETIME NULL DEFAULT NULL,
                                               `hidden_by_user_id` INT(11) NULL DEFAULT NULL,
                                               PRIMARY KEY (`id`),
                                               INDEX `idx_post_metadata_hidden` (`hidden` ASC),
                                               INDEX `idx_post_metadata_user_deleted` (`user_deleted` ASC),
                                               INDEX `idx_post_metadata_deleted_at` (`deleted_at` ASC),
                                               INDEX `fk_post_metadata_hidden_by_user` (`hidden_by_user_id` ASC),
                                               CONSTRAINT `fk_post_metadata_hidden_by_user`
                                                   FOREIGN KEY (`hidden_by_user_id`)
                                                       REFERENCES `user` (`id`)
                                                       ON DELETE SET NULL)
    ENGINE = InnoDB
    AUTO_INCREMENT = 62
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `refresh_token`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `refresh_token` (
                                               `id` INT(11) NOT NULL AUTO_INCREMENT,
                                               `user_id` INT(11) NOT NULL,
                                               `token_hash` VARCHAR(64) NOT NULL,
                                               `expires_at` DATETIME(6) NOT NULL,
                                               `revoked` TINYINT(1) NOT NULL DEFAULT '0',
                                               `created_at` DATETIME(6) NOT NULL,
                                               PRIMARY KEY (`id`),
                                               UNIQUE INDEX `uq_refresh_token_hash` (`token_hash` ASC),
                                               INDEX `idx_refresh_token_user_id` (`user_id` ASC),
                                               CONSTRAINT `fk_refresh_token_user`
                                                   FOREIGN KEY (`user_id`)
                                                       REFERENCES `user` (`id`)
                                                       ON DELETE CASCADE)
    ENGINE = InnoDB
    AUTO_INCREMENT = 72
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `thread`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `thread` (
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
                                                REFERENCES `user` (`id`),
                                        CONSTRAINT `fk_thread_post_metadata`
                                            FOREIGN KEY (`metadata`)
                                                REFERENCES `post_metadata` (`id`))
    ENGINE = InnoDB
    AUTO_INCREMENT = 61
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `thread_like`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `thread_like` (
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
                                                     REFERENCES `user` (`id`),
                                             CONSTRAINT `fk_thread_like_thread1`
                                                 FOREIGN KEY (`thread`)
                                                     REFERENCES `thread` (`id`))
    ENGINE = InnoDB
    AUTO_INCREMENT = 115
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `thread_reply`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `thread_reply` (
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
                                                      REFERENCES `user` (`id`),
                                              CONSTRAINT `fk_thread_reply_post_metadata1`
                                                  FOREIGN KEY (`metadata`)
                                                      REFERENCES `post_metadata` (`id`),
                                              CONSTRAINT `fk_thread_reply_thread_id`
                                                  FOREIGN KEY (`thread_id`)
                                                      REFERENCES `thread` (`id`))
    ENGINE = InnoDB
    AUTO_INCREMENT = 11
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `thread_reply_like`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `thread_reply_like` (
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
                                                           REFERENCES `thread_reply` (`id`),
                                                   CONSTRAINT `fk_thread_reply_like_User1`
                                                       FOREIGN KEY (`user`)
                                                           REFERENCES `user` (`id`),
                                                   CONSTRAINT `fk_thread_reply_like_thread_reply1`
                                                       FOREIGN KEY (`thread`)
                                                           REFERENCES `thread_reply` (`id`))
    ENGINE = InnoDB
    AUTO_INCREMENT = 10
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `thread_tag`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `thread_tag` (
                                            `id` INT(11) NOT NULL AUTO_INCREMENT,
                                            `name` VARCHAR(255) NULL DEFAULT NULL,
                                            `thread` INT(11) NOT NULL,
                                            PRIMARY KEY (`id`),
                                            INDEX `thread` (`thread` ASC),
                                            CONSTRAINT `fk_thread_tag_thread1`
                                                FOREIGN KEY (`thread`)
                                                    REFERENCES `thread` (`id`))
    ENGINE = InnoDB
    AUTO_INCREMENT = 48
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- Table `token_auth`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `token_auth` (
                                            `id` INT(11) NOT NULL AUTO_INCREMENT,
                                            `token` VARCHAR(100) NOT NULL,
                                            `expires_at` DATETIME NULL DEFAULT NULL COMMENT 'If null, token is indefinite',
                                            `valid_for_endpoint` VARCHAR(200) NOT NULL,
                                            `generated_by_user_id` INT(11) NOT NULL,
                                            `target_user_id` INT(11) NULL DEFAULT NULL,
                                            `process_id` VARCHAR(100) NULL DEFAULT NULL COMMENT 'When null, token has no sibling group (e.g. persistent unsubscribe token)',
                                            PRIMARY KEY (`id`),
                                            INDEX `fk_token_auth_user1_idx` (`generated_by_user_id` ASC),
                                            INDEX `fk_token_auth_user2_idx` (`target_user_id` ASC),
                                            CONSTRAINT `fk_token_auth_user1`
                                                FOREIGN KEY (`generated_by_user_id`)
                                                    REFERENCES `user` (`id`)
                                                    ON DELETE NO ACTION
                                                    ON UPDATE NO ACTION,
                                            CONSTRAINT `fk_token_auth_user2`
                                                FOREIGN KEY (`target_user_id`)
                                                    REFERENCES `user` (`id`)
                                                    ON DELETE NO ACTION
                                                    ON UPDATE NO ACTION)
    ENGINE = InnoDB
    AUTO_INCREMENT = 23
    DEFAULT CHARACTER SET = utf8;


-- -----------------------------------------------------
-- procedure describe_and_count
-- -----------------------------------------------------

DELIMITER $$
CREATE DEFINER=`developer`@`%` PROCEDURE `describe_and_count`()
BEGIN
    SELECT COUNT(*) AS user_count FROM `user`;
    DESCRIBE `user`;
END$$

DELIMITER ;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
