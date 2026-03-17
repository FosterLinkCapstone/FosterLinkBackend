-- ============================================================
-- V037: Retroactive GDPR Article 17 erasure pass
-- ============================================================
-- One-time retroactive erasure of PII for user rows that are
-- already marked account_deleted = 1 but still hold identifying
-- data. This covers accounts deleted before the anonymizeUser()
-- fix (which now nulls first_name/last_name and sets an email
-- pseudonym instead of storing the HMAC hash in the email field).
--
-- The email column is nullable (VARCHAR(255) NULL DEFAULT NULL)
-- so we could null it. However, to match the pseudonym approach
-- now used by anonymizeUser(), we set email to
-- 'deleted-<id>@deleted.invalid' for any deleted row that does
-- not already carry that pattern. This keeps application code
-- that reads the email column after deletion consistent.
-- ============================================================

-- Erase nullable PII columns unconditionally for all deleted accounts.
-- Note: unsubscribe_token was dropped and replaced by unsubscribe_token_hash in V036.
UPDATE `user`
SET
    first_name                  = NULL,
    last_name                   = NULL,
    phone_number                = NULL,
    profile_picture_url         = NULL,
    unsubscribe_token_hash      = NULL,
    unsubscribe_token_issued_at = NULL
WHERE account_deleted = 1
  AND (
      first_name                  IS NOT NULL OR
      last_name                   IS NOT NULL OR
      phone_number                IS NOT NULL OR
      profile_picture_url         IS NOT NULL OR
      unsubscribe_token_hash      IS NOT NULL OR
      unsubscribe_token_issued_at IS NOT NULL
  );

-- Pseudonymize email for deleted rows that do not yet carry the
-- deleted-<id>@deleted.invalid marker (i.e. rows whose email is
-- still a real address or the old HMAC hash string).
-- email is nullable, but we use a pseudonym rather than NULL so
-- that the column remains non-empty for downstream queries.
UPDATE `user`
SET email = CONCAT('deleted-', id, '@deleted.invalid')
WHERE account_deleted = 1
  AND (email IS NULL OR email NOT LIKE 'deleted-%@deleted.invalid');
