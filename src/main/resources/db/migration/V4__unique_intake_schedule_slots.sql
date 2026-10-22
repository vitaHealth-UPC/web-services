-- Preserve every existing identifier and definitive outcome; ambiguous history requires diagnosis.
LOCK TABLE intake_intakes IN ACCESS EXCLUSIVE MODE;
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM intake_intakes
        GROUP BY treatment_id, scheduled_at HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate intake schedule slots require diagnosis before migration';
    END IF;
END $$;
ALTER TABLE intake_intakes ADD CONSTRAINT uk_intake_treatment_scheduled_at
    UNIQUE (treatment_id, scheduled_at);
