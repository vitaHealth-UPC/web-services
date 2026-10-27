-- External Firebase identities must be uniquely bound to one Tata caregiver account.
-- No existing account is linked by matching an email address.
CREATE TABLE identity_social_links (
    id VARCHAR(36) PRIMARY KEY,
    issuer VARCHAR(255) NOT NULL,
    external_subject VARCHAR(128) NOT NULL,
    account_id VARCHAR(255) NOT NULL REFERENCES identity_accounts(id),
    provider VARCHAR(16) NOT NULL,
    CONSTRAINT uk_identity_social_issuer_subject UNIQUE (issuer, external_subject),
    CONSTRAINT ck_identity_social_provider CHECK (provider IN ('GOOGLE', 'FACEBOOK'))
);
CREATE INDEX idx_identity_social_link_account ON identity_social_links(account_id);
