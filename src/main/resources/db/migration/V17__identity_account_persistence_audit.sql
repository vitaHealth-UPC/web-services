-- Existing rows record when technical auditing is introduced, not account creation time.
ALTER TABLE identity_accounts ADD COLUMN persisted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE identity_accounts ADD COLUMN modified_at TIMESTAMP WITH TIME ZONE;
UPDATE identity_accounts
SET persisted_at = CURRENT_TIMESTAMP, modified_at = CURRENT_TIMESTAMP;
ALTER TABLE identity_accounts ALTER COLUMN persisted_at SET NOT NULL;
ALTER TABLE identity_accounts ALTER COLUMN modified_at SET NOT NULL;
