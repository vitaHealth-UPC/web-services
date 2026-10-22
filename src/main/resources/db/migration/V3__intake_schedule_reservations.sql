-- Technical per-treatment coordination keys; no clinical intake rows are rewritten.
CREATE TABLE intake_schedule_locks (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    treatment_id VARCHAR(36) NOT NULL,
    CONSTRAINT uk_intake_schedule_lock_treatment UNIQUE (treatment_id)
);
