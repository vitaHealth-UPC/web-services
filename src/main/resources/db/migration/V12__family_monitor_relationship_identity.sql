-- A relationship owns one follow-up; subsequent relationships retain their own history.
-- Existing duplicates require explicit reconciliation instead of automatic evidence deletion.
ALTER TABLE family_monitors
    ADD CONSTRAINT uq_family_monitor_care_link UNIQUE (care_link_id);
