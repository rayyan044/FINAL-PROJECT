CREATE TABLE IF NOT EXISTS revoked_refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_revoked_refresh_tokens_expires_at
    ON revoked_refresh_tokens (expires_at);
