-- Durable decrement intent belongs to Inventory; references to Intake and Treatment stay logical.
CREATE TABLE inventory_consumption_requests (
    intake_id VARCHAR(36) PRIMARY KEY,
    medication_id VARCHAR(36) NOT NULL,
    older_adult_id VARCHAR(36) NOT NULL,
    confirmed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempts INTEGER NOT NULL,
    CONSTRAINT ck_inventory_request_attempts CHECK (attempts >= 0),
    CONSTRAINT ck_inventory_request_times CHECK
        (created_at >= confirmed_at AND next_attempt_at >= created_at)
);
CREATE INDEX ix_inventory_consumption_due
    ON inventory_consumption_requests (next_attempt_at, intake_id);
