-- PostgreSQL diagnostics before adopting the Treatment Management schema.
-- Reports counts only. Never repairs rows, regenerates IDs, or changes clinical history.
BEGIN TRANSACTION READ ONLY;

WITH frequency_schedules AS (
    SELECT t.id,
        LOWER(TRANSLATE(BTRIM(REGEXP_REPLACE(NORMALIZE(t.frequency, NFC), '\s+', ' ', 'g')),
                        'ÁÉÍÓÚÜÑáéíóúüñ', 'AEIOUUNaeiouun')) AS frequency,
        ARRAY_AGG(DISTINCT EXTRACT(EPOCH FROM s.scheduled_time)
                  ORDER BY EXTRACT(EPOCH FROM s.scheduled_time))
            FILTER (WHERE s.scheduled_time IS NOT NULL) AS times
    FROM treatment_treatments t
    LEFT JOIN treatment_schedule_times s ON s.treatment_id = t.id
    WHERE t.medication_id IS NOT NULL
    GROUP BY t.id, t.frequency
)
SELECT 'regimen_without_medication' AS finding, COUNT(*) AS affected_rows
FROM treatment_treatments t
WHERE t.medication_id IS NULL
  AND (t.dose IS NOT NULL OR t.frequency IS NOT NULL OR t.reminder_lead_minutes IS NOT NULL
       OR NULLIF(BTRIM(t.instructions), '') IS NOT NULL
       OR EXISTS (SELECT 1 FROM treatment_schedule_times s WHERE s.treatment_id = t.id))
UNION ALL
SELECT 'incomplete_configured_regimen', COUNT(*) FROM treatment_treatments t
WHERE t.medication_id IS NOT NULL
  AND (NULLIF(BTRIM(t.medication_id), '') IS NULL
       OR NULLIF(BTRIM(t.dose), '') IS NULL OR LENGTH(t.dose) > 100
       OR NULLIF(BTRIM(t.frequency), '') IS NULL OR LENGTH(t.frequency) > 100
       OR LENGTH(t.instructions) > 500
       OR t.reminder_lead_minutes IS NULL OR t.reminder_lead_minutes NOT BETWEEN 0 AND 1440
       OR NOT EXISTS (SELECT 1 FROM treatment_schedule_times s WHERE s.treatment_id = t.id))
UNION ALL
SELECT 'invalid_lifecycle_evidence', COUNT(*) FROM treatment_treatments t
WHERE t.status IS NULL OR t.status NOT IN ('DRAFT', 'ACTIVE', 'PAUSED')
   OR (t.status IN ('ACTIVE', 'PAUSED') AND t.medication_id IS NULL)
UNION ALL
SELECT 'missing_medications', COUNT(*) FROM treatment_treatments t
LEFT JOIN treatment_medications m ON m.id = t.medication_id
WHERE t.medication_id IS NOT NULL AND m.id IS NULL
UNION ALL
SELECT 'foreign_medication_owners', COUNT(*) FROM treatment_treatments t
JOIN treatment_medications m ON m.id = t.medication_id
WHERE t.older_adult_id IS DISTINCT FROM m.older_adult_id
UNION ALL
SELECT 'active_treatments_with_inactive_medication', COUNT(*) FROM treatment_treatments t
JOIN treatment_medications m ON m.id = t.medication_id
WHERE t.status = 'ACTIVE' AND NOT m.active
UNION ALL
SELECT 'orphan_schedule_rows', COUNT(*) FROM treatment_schedule_times s
LEFT JOIN treatment_treatments t ON t.id = s.treatment_id WHERE t.id IS NULL
UNION ALL
SELECT 'duplicate_schedule_times', COUNT(*) FROM (
    SELECT treatment_id, scheduled_time FROM treatment_schedule_times
    GROUP BY treatment_id, scheduled_time HAVING COUNT(*) > 1
) duplicates
UNION ALL
SELECT 'invalid_schedule_order', COUNT(*) FROM (
    SELECT treatment_id FROM treatment_schedule_times GROUP BY treatment_id
    HAVING COUNT(schedule_order) <> COUNT(*) OR COUNT(DISTINCT schedule_order) <> COUNT(*)
        OR MIN(schedule_order) <> 0 OR MAX(schedule_order) <> COUNT(*) - 1
) invalid_order
UNION ALL
SELECT 'missing_schedule_times', COUNT(*) FROM treatment_schedule_times WHERE scheduled_time IS NULL
UNION ALL
SELECT 'invalid_frequency_schedule', COUNT(*) FROM frequency_schedules
WHERE NOT COALESCE(
    (frequency IN ('daily', 'cada dia', 'diaria', 'segun indicacion medica')
        AND CARDINALITY(times) >= 1)
    OR (frequency IN ('una vez al dia', 'cada 24 horas') AND CARDINALITY(times) = 1)
    OR (frequency = 'cada 12 horas' AND CARDINALITY(times) = 2
        AND times[2] - times[1] = 43200)
    OR (frequency = 'cada 8 horas' AND CARDINALITY(times) = 3
        AND times[2] - times[1] = 28800 AND times[3] - times[2] = 28800), FALSE);

ROLLBACK;
