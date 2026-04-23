CREATE TABLE IF NOT EXISTS system_parameters (
    param_key VARCHAR(128) PRIMARY KEY,
    value_type VARCHAR(16) NOT NULL,
    param_value TEXT NOT NULL,
    runtime_applicable BOOLEAN NOT NULL,
    version INTEGER NOT NULL CHECK (version >= 1),
    description TEXT,
    updated_by_actor VARCHAR(128),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS system_parameters_key_uq ON system_parameters(param_key);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    actor_user_id VARCHAR(128),
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id VARCHAR(128) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS audit_logs_target_idx
    ON audit_logs(target_type, target_id, created_at DESC);
