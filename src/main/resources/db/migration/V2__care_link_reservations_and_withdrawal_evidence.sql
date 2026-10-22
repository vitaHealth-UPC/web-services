-- Applies after the verified legacy schema baseline, in Flyway's transaction.
-- Reject ambiguous evidence before changing the schema. No row is deleted or renumbered.
LOCK TABLE care_links IN ACCESS EXCLUSIVE MODE;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM care_links
        WHERE created_at IS NULL OR updated_at IS NULL OR code_expires_at IS NULL
           OR updated_at < created_at OR code_expires_at <= created_at
           OR (code_used_at IS NOT NULL AND
               (code_used_at < created_at OR code_used_at > updated_at OR code_used_at >= code_expires_at))
           OR (consent_recorded_at IS NOT NULL AND
               (code_used_at IS NULL OR consent_recorded_at < code_used_at OR consent_recorded_at > updated_at))
           OR (confirmed_at IS NOT NULL AND
               (code_used_at IS NULL OR confirmed_at < code_used_at OR confirmed_at > updated_at))
           OR (consent_granted AND consent_recorded_at IS NULL)
           OR CASE status
               WHEN 'PENDING' THEN code_used_at IS NOT NULL OR consent_recorded_at IS NOT NULL OR confirmed_at IS NOT NULL
               WHEN 'AWAITING_CONSENT' THEN code_used_at IS NULL OR confirmed_at IS NOT NULL
                   OR (consent_recorded_at IS NOT NULL AND NOT consent_granted)
               WHEN 'CONFIRMED' THEN code_used_at IS NULL OR NOT consent_granted
                   OR consent_recorded_at IS NULL OR confirmed_at IS NULL OR confirmed_at < consent_recorded_at
               WHEN 'REVOKED' THEN code_used_at IS NULL OR consent_granted OR consent_recorded_at IS NULL
                   OR (confirmed_at IS NOT NULL AND consent_recorded_at < confirmed_at)
               ELSE true
           END
    ) THEN
        RAISE EXCEPTION 'Care Link migration: inconsistent lifecycle evidence requires review';
    END IF;
    IF EXISTS (
        SELECT 1 FROM care_links
        WHERE status IN ('AWAITING_CONSENT', 'CONFIRMED')
           OR (status = 'PENDING' AND code_used_at IS NULL AND code_expires_at > CURRENT_TIMESTAMP)
        GROUP BY caregiver_id, older_adult_id HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Care Link migration: duplicate open member pairs require review';
    END IF;
    IF EXISTS (
        SELECT 1 FROM care_links
        WHERE status = 'PENDING' AND code_used_at IS NULL AND code_expires_at > CURRENT_TIMESTAMP
        GROUP BY linking_code HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Care Link migration: duplicate live codes require review';
    END IF;
END $$;

ALTER TABLE care_links ADD COLUMN reserved_linking_code varchar(32);
ALTER TABLE care_links ADD COLUMN reserved_caregiver_id varchar(36);
ALTER TABLE care_links ADD COLUMN reserved_older_adult_id varchar(36);

UPDATE care_links
SET reserved_linking_code = CASE
        WHEN status = 'PENDING' AND code_used_at IS NULL AND code_expires_at > CURRENT_TIMESTAMP
        THEN linking_code ELSE NULL END,
    reserved_caregiver_id = CASE
        WHEN status IN ('AWAITING_CONSENT', 'CONFIRMED')
          OR (status = 'PENDING' AND code_used_at IS NULL AND code_expires_at > CURRENT_TIMESTAMP)
        THEN caregiver_id ELSE NULL END,
    reserved_older_adult_id = CASE
        WHEN status IN ('AWAITING_CONSENT', 'CONFIRMED')
          OR (status = 'PENDING' AND code_used_at IS NULL AND code_expires_at > CURRENT_TIMESTAMP)
        THEN older_adult_id ELSE NULL END;

ALTER TABLE care_links ADD CONSTRAINT uk_care_link_reserved_code UNIQUE (reserved_linking_code);
ALTER TABLE care_links ADD CONSTRAINT uk_care_link_reserved_members
    UNIQUE (reserved_caregiver_id, reserved_older_adult_id);

CREATE TABLE care_link_code_namespaces (
    id varchar(36) PRIMARY KEY,
    code_namespace varchar(32) NOT NULL,
    CONSTRAINT uk_care_link_code_namespace UNIQUE (code_namespace)
);

CREATE TABLE care_link_consent_withdrawals (
    care_link_id varchar(36) PRIMARY KEY,
    caregiver_id varchar(36) NOT NULL,
    older_adult_id varchar(36) NOT NULL,
    approved_at timestamp(6) with time zone NOT NULL,
    withdrawn_at timestamp(6) with time zone NOT NULL
);
-- Legacy rows do not retain the original approval after withdrawal.
-- Leave historical timestamps untouched; never invent approved_at from confirmed_at.
