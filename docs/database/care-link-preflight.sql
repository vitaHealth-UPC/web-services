-- PostgreSQL preflight for the legacy Care Link tables.
-- Execute before schema adoption; reports counts without disclosing credentials or contact data.
-- Does not choose duplicate winners, alter identifiers, or modify historical decisions.
BEGIN TRANSACTION READ ONLY;

WITH open_links AS (
    SELECT caregiver_id, older_adult_id, linking_code
    FROM care_links
    WHERE status IN ('AWAITING_CONSENT', 'CONFIRMED')
       OR (status = 'PENDING' AND code_used_at IS NULL AND code_expires_at > CURRENT_TIMESTAMP)
), duplicate_pairs AS (
    SELECT caregiver_id, older_adult_id
    FROM open_links
    GROUP BY caregiver_id, older_adult_id
    HAVING COUNT(*) > 1
), duplicate_codes AS (
    SELECT linking_code
    FROM care_links
    WHERE status = 'PENDING' AND code_used_at IS NULL AND code_expires_at > CURRENT_TIMESTAMP
    GROUP BY linking_code
    HAVING COUNT(*) > 1
)
SELECT 'duplicate_open_member_pairs' AS finding, COUNT(*) AS affected_groups FROM duplicate_pairs
UNION ALL
SELECT 'duplicate_live_codes', COUNT(*) FROM duplicate_codes
UNION ALL
SELECT 'missing_adult_profiles', COUNT(*)
FROM care_links link LEFT JOIN older_adult_profiles profile ON profile.id = link.older_adult_id
WHERE profile.id IS NULL
UNION ALL
SELECT 'partial_emergency_contacts', COUNT(*) FROM older_adult_profiles
WHERE (emergency_contact_name IS NOT NULL OR emergency_contact_relationship IS NOT NULL
       OR emergency_contact_phone IS NOT NULL)
  AND (emergency_contact_name IS NULL OR emergency_contact_relationship IS NULL
       OR emergency_contact_phone IS NULL)
UNION ALL
SELECT 'invalid_relationship_chronology', COUNT(*) FROM care_links
WHERE updated_at < created_at OR code_expires_at <= created_at
   OR code_used_at < created_at OR code_used_at >= code_expires_at
   OR consent_recorded_at < code_used_at
   OR (status = 'CONFIRMED' AND confirmed_at < consent_recorded_at)
   OR (status = 'REVOKED' AND consent_recorded_at < confirmed_at)
UNION ALL
SELECT 'confirmed_without_approval', COUNT(*) FROM care_links
WHERE status = 'CONFIRMED'
  AND (NOT consent_granted OR consent_recorded_at IS NULL OR confirmed_at IS NULL OR code_used_at IS NULL);

ROLLBACK;
