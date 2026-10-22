-- Intention commit is atomic with its source case; fanout completion is not sent evidence.
CREATE TABLE omission_notification_intents (
    id VARCHAR(36) PRIMARY KEY,
    omission_case_id BIGINT NOT NULL REFERENCES omission_cases(id),
    kind VARCHAR(32) NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    state VARCHAR(32) NOT NULL,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    lease_token VARCHAR(36),
    lease_until TIMESTAMP WITH TIME ZONE,
    lease_started_at TIMESTAMP WITH TIME ZONE,
    caregiver_after_id VARCHAR(255) NOT NULL,
    current_caregiver_id VARCHAR(255),
    device_after_id VARCHAR(36) NOT NULL,
    CONSTRAINT uk_omission_notification_source_kind UNIQUE (omission_case_id, kind),
    CONSTRAINT ck_omission_notification_kind CHECK (kind IN ('REINFORCED_REMINDER', 'CAREGIVER_ALERT')),
    CONSTRAINT ck_omission_notification_state CHECK (state IN ('READY', 'FANOUT_COMPLETE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_omission_notification_lease CHECK ((lease_token IS NULL) = (lease_until IS NULL) AND (lease_token IS NULL) = (lease_started_at IS NULL)),
    CONSTRAINT ck_omission_notification_lease_time CHECK (lease_started_at IS NULL OR (lease_started_at >= requested_at AND lease_until > lease_started_at))
);
CREATE INDEX idx_omission_notification_fanout_due ON omission_notification_intents (state, next_attempt_at, id);
