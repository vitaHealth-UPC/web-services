-- Read-only PostgreSQL checks; no clinical values, identifiers or repair decisions are emitted.
BEGIN TRANSACTION READ ONLY;
SELECT 'duplicate_treatment_slots' AS finding, COUNT(*) AS affected_groups FROM (
    SELECT treatment_id, scheduled_at FROM intake_intakes
    GROUP BY treatment_id, scheduled_at HAVING COUNT(*) > 1
) duplicates
UNION ALL
SELECT 'incomplete_confirmation_evidence', COUNT(*) FROM intake_intakes
WHERE (status IN ('CONFIRMED', 'LATE') AND (confirmed_at IS NULL OR confirmation_channel IS NULL))
   OR (status IN ('PENDING', 'OMITTED') AND (confirmed_at IS NOT NULL OR confirmation_channel IS NOT NULL))
UNION ALL
SELECT 'invalid_intake_status', COUNT(*) FROM intake_intakes
WHERE status IS NULL OR status NOT IN ('PENDING', 'CONFIRMED', 'LATE', 'OMITTED');
ROLLBACK;
