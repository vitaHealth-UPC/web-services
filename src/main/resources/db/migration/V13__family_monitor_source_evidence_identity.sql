-- Source evidence is unique within its follow-up, while several caregivers may share the source.
-- Duplicate historical evidence requires explicit reconciliation; no history is discarded here.
ALTER TABLE alert_summaries
    ADD CONSTRAINT uq_follow_up_omitted_intake UNIQUE (family_monitor_id, intake_id);
ALTER TABLE low_stock_notices
    ADD CONSTRAINT uq_follow_up_stock_medication UNIQUE (family_monitor_id, medication_id);
ALTER TABLE adherence_insights
    ADD CONSTRAINT uq_follow_up_adherence_pattern
        UNIQUE (family_monitor_id, medication_id, first_day, last_day);
