-- Backfill: rows already user_deleted=1 start the 90-day clock from migration time.
UPDATE post_metadata
SET deleted_at = NOW()
WHERE user_deleted = 1
  AND deleted_at IS NULL;
