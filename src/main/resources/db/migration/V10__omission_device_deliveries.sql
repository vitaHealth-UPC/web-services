-- Each selected installation retains its own provider outcome; no raw device address is copied.
CREATE TABLE omission_device_deliveries (
    id VARCHAR(36) PRIMARY KEY,
    intent_id VARCHAR(36) NOT NULL REFERENCES omission_notification_intents(id),
    installation_id VARCHAR(36) NOT NULL,
    session_id VARCHAR(255) NOT NULL,
    recipient_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    state VARCHAR(24) NOT NULL,
    attempts INTEGER NOT NULL,
    lease_token VARCHAR(36),
    lease_started_at TIMESTAMP WITH TIME ZONE,
    lease_until TIMESTAMP WITH TIME ZONE,
    provider_receipt VARCHAR(512),
    accepted_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_omission_delivery_intent_installation UNIQUE (intent_id, installation_id),
    CONSTRAINT ck_omission_device_delivery_state CHECK (state IN ('READY', 'ACCEPTED', 'CANCELLED')),
    CONSTRAINT ck_omission_device_delivery_attempts CHECK (attempts >= 0),
    CONSTRAINT ck_omission_device_delivery_lease CHECK ((lease_token IS NULL) = (lease_started_at IS NULL) AND (lease_token IS NULL) = (lease_until IS NULL)),
    CONSTRAINT ck_omission_device_delivery_times CHECK (next_attempt_at >= created_at AND (lease_started_at IS NULL OR (lease_started_at >= created_at AND lease_until > lease_started_at))),
    CONSTRAINT ck_omission_device_delivery_acceptance CHECK ((state = 'ACCEPTED' AND provider_receipt IS NOT NULL AND length(trim(provider_receipt)) > 0 AND accepted_at IS NOT NULL AND accepted_at >= created_at) OR (state <> 'ACCEPTED' AND provider_receipt IS NULL AND accepted_at IS NULL))
);
CREATE INDEX idx_omission_device_delivery_due ON omission_device_deliveries (state, next_attempt_at, id);
