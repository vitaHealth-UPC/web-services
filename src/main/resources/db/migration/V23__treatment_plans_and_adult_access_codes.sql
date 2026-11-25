-- Regimens of one multi-medication treatment share a plan; NULL keeps a row as its own plan.
ALTER TABLE treatment_treatments ADD COLUMN plan_id VARCHAR(36);

-- Single-use codes that let an adult sign in again on a new or reinstalled device.
CREATE TABLE care_link_adult_access_codes (
    id VARCHAR(36) PRIMARY KEY,
    care_link_id VARCHAR(36) NOT NULL,
    older_adult_id VARCHAR(36) NOT NULL,
    issued_by VARCHAR(36) NOT NULL,
    code VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_care_link_adult_access_code UNIQUE (code)
);
CREATE INDEX idx_adult_access_code_link ON care_link_adult_access_codes (care_link_id);
