CREATE TABLE family_stock_catchups (
    family_monitor_id BIGINT PRIMARY KEY REFERENCES family_monitors(id) ON DELETE CASCADE,
    after_medication_id VARCHAR(36) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts INTEGER NOT NULL CHECK (attempts >= 0),
    CHECK (next_attempt_at >= requested_at)
);
CREATE INDEX ix_family_stock_catchups_due ON family_stock_catchups(next_attempt_at, family_monitor_id);
CREATE INDEX ix_medications_adult_identity ON treatment_medications(older_adult_id, id);

