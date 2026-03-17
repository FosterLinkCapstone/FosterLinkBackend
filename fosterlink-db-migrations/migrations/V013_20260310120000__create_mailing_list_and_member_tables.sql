CREATE TABLE `mailing_list` (
    `id`   INT         NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
);

CREATE TABLE `mailing_list_member` (
    `id`              INT NOT NULL AUTO_INCREMENT,
    `user_id`         INT NOT NULL,
    `mailing_list_id` INT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_mailing_list_member_user_list` (`user_id`, `mailing_list_id`),
    KEY `idx_mailing_list_member_user_id` (`user_id`),
    KEY `idx_mailing_list_member_mailing_list_id` (`mailing_list_id`),
    CONSTRAINT `fk_mailing_list_member_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_mailing_list_member_mailing_list` FOREIGN KEY (`mailing_list_id`) REFERENCES `mailing_list` (`id`) ON DELETE CASCADE
);
