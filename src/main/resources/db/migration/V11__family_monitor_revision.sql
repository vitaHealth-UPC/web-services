-- Existing follow-up rows start at revision zero; JPA advances revision on each aggregate write.
ALTER TABLE family_monitors ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;
ALTER TABLE family_monitors ADD CONSTRAINT ck_family_monitor_revision CHECK (revision >= 0);
