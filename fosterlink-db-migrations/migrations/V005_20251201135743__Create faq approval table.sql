CREATE TABLE `fosterlink_dev`.`faq_approval` ( `id` INT NOT NULL AUTO_INCREMENT , `faq_id` INT NOT NULL , `approved` BOOLEAN NOT NULL , `approved_by_id` INT NULL , PRIMARY KEY (`id`)) ENGINE = InnoDB;
ALTER TABLE faq_approval ADD FOREIGN KEY (faq_id) REFERENCES faq(id);
ALTER TABLE faq_approval ADD FOREIGN KEY (approved_by_id) REFERENCES user(id);