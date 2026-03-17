CREATE TABLE `refresh_token` (
    `id`          INT          NOT NULL AUTO_INCREMENT,
    `user_id`     INT          NOT NULL,
    `token_hash`  VARCHAR(64)  NOT NULL,
    `expires_at`  DATETIME(6)  NOT NULL,
    `revoked`     TINYINT(1)   NOT NULL DEFAULT 0,
    `created_at`  DATETIME(6)  NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_refresh_token_hash` (`token_hash`),
    KEY `idx_refresh_token_user_id` (`user_id`),
    CONSTRAINT `fk_refresh_token_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
);
