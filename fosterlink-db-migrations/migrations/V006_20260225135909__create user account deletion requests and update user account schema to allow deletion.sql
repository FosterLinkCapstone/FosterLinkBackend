ALTER TABLE `user` 
ADD COLUMN `account_deleted` TINYINT(4) NOT NULL DEFAULT 0 AFTER `password`,
CHANGE COLUMN `password` `password` VARCHAR(255) NOT NULL;

CREATE TABLE IF NOT EXISTS `account_deletion_request` (
  `id` INT(11) NOT NULL,
  `requested_by_email_hash` VARCHAR(255) NOT NULL,
  `requested_at` DATETIME NOT NULL DEFAULT NOW(),
  `reviewed_at` DATETIME NULL DEFAULT NULL,
  `auto_approved` TINYINT(4) NOT NULL DEFAULT 0,
  `auto_approve_by` DATETIME NOT NULL,
  `approved` TINYINT(4) NOT NULL DEFAULT 0,
  `delay_note` VARCHAR(1500) NULL DEFAULT NULL,
  `requested_by` INT(11) NOT NULL,
  `reviewed_by` INT(11) NULL DEFAULT NULL,
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
DEFAULT CHARACTER SET = utf8
