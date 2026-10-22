-- Support bounded device scans scoped to live Identity session ownership.
CREATE INDEX idx_identity_push_device_session ON identity_push_devices (session_id);
CREATE INDEX idx_identity_session_subject_role ON identity_account_sessions (account_id, session_role, expires_at);
