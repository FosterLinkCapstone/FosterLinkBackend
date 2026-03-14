CREATE TABLE `consent_record` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT,
    `user_id`        INT           NOT NULL,
    `consent_type`   VARCHAR(50)   NOT NULL,
    `granted`        TINYINT(1)    NOT NULL,
    `timestamp`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `policy_version` VARCHAR(20)   NULL DEFAULT NULL,
    `mechanism`      VARCHAR(50)   NULL DEFAULT NULL,
    `ip_address`     VARCHAR(45)   NULL DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_consent_record_user_id` (`user_id`),
    CONSTRAINT `fk_consent_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
);
