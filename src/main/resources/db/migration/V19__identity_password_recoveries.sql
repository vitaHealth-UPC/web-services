CREATE TABLE IF NOT EXISTS identity_password_recoveries (
    account_id VARCHAR(255) PRIMARY KEY REFERENCES identity_accounts(id),
    code_hash VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    failed_attempts INTEGER NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_password_recovery_attempts CHECK (failed_attempts BETWEEN 0 AND 5),
    CONSTRAINT ck_password_recovery_lifetime CHECK (expires_at > issued_at),
    CONSTRAINT ck_password_recovery_consumption CHECK (
        consumed_at IS NULL OR (consumed_at >= issued_at AND consumed_at < expires_at AND failed_attempts < 5)
    )
);
