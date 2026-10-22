-- Device addresses are owned by revocable Identity sessions; no bearer credentials are stored.
CREATE TABLE identity_push_devices (
    installation_id VARCHAR(36) PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    token VARCHAR(4096) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT uk_identity_push_device_token UNIQUE (token_hash),
    CONSTRAINT ck_identity_push_device_write_time CHECK (updated_at >= created_at)
);
