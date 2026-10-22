-- All-null metadata retains the original UTC local-time interpretation.
ALTER TABLE treatment_treatments ADD COLUMN recurrence_mode VARCHAR(30);
ALTER TABLE treatment_treatments ADD COLUMN recurrence_zone VARCHAR(100);
ALTER TABLE treatment_treatments ADD COLUMN recurrence_anchor TIMESTAMP WITH TIME ZONE;
ALTER TABLE treatment_treatments ADD CONSTRAINT ck_treatment_recurrence_metadata CHECK (
    (recurrence_mode IS NULL AND recurrence_zone IS NULL AND recurrence_anchor IS NULL)
    OR (medication_id IS NOT NULL AND recurrence_mode IS NOT NULL
        AND recurrence_zone IS NOT NULL AND length(trim(recurrence_zone)) > 0
        AND ((recurrence_mode = 'LOCAL_TIMES' AND recurrence_anchor IS NULL)
          OR (recurrence_mode = 'FIXED_INTERVAL' AND recurrence_anchor IS NOT NULL)))
);
