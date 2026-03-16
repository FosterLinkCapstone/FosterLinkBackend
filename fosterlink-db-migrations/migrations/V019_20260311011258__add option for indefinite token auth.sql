ALTER TABLE `token_auth` CHANGE `expires_at` `expires_at` DATETIME NULL COMMENT 'If null, token is indefinite';
