CREATE TABLE IF NOT EXISTS device_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token TEXT NOT NULL,
    platform VARCHAR(32) NOT NULL,
    device_id VARCHAR(128),
    app_version VARCHAR(64),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT device_tokens_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT device_tokens_platform_ck CHECK (platform IN ('ANDROID', 'IOS', 'WEB', 'UNKNOWN'))
);

CREATE UNIQUE INDEX IF NOT EXISTS device_tokens_token_uidx
    ON device_tokens(token);

CREATE INDEX IF NOT EXISTS device_tokens_user_enabled_idx
    ON device_tokens(user_id, enabled, last_seen_at DESC);
