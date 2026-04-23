CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(32) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(512),
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS users_username_uq ON users(username);
CREATE UNIQUE INDEX IF NOT EXISTS users_email_uq ON users(email);
CREATE UNIQUE INDEX IF NOT EXISTS users_phone_uq ON users(phone_number);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_jti VARCHAR(128) NOT NULL,
    token_family VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    replaced_by_jti VARCHAR(128),
    CONSTRAINT refresh_tokens_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS refresh_tokens_jti_uq ON refresh_tokens(token_jti);
CREATE INDEX IF NOT EXISTS refresh_tokens_family_idx ON refresh_tokens(token_family);
CREATE INDEX IF NOT EXISTS refresh_tokens_user_expires_idx ON refresh_tokens(user_id, expires_at DESC);

CREATE TABLE IF NOT EXISTS password_reset_challenges (
    id UUID PRIMARY KEY,
    challenge_token VARCHAR(128) NOT NULL,
    user_id UUID NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT prc_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS prc_challenge_uq ON password_reset_challenges(challenge_token);
CREATE INDEX IF NOT EXISTS prc_user_created_idx ON password_reset_challenges(user_id, created_at DESC);
