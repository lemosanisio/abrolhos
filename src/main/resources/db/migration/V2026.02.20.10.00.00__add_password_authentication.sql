-- Migration: Add password authentication support
-- Adds password_hash to users and creates password_reset_tokens table.
-- password_hash is nullable to support the migration period for existing TOTP-only users.

-- Add password_hash column to users table (nullable initially for migration)
ALTER TABLE users
ADD COLUMN password_hash VARCHAR(60) NULL;

-- Create password_reset_tokens table
CREATE TABLE password_reset_tokens (
    id          VARCHAR(26)              NOT NULL,
    user_id     VARCHAR(26)              NOT NULL,
    token       VARCHAR(64)              NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_user   FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for efficient lookups and cleanup queries
CREATE INDEX idx_password_reset_token      ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_expires_at ON password_reset_tokens(expires_at);
CREATE INDEX idx_password_reset_user_id    ON password_reset_tokens(user_id);

-- Documentation comments
COMMENT ON COLUMN users.password_hash IS 'Bcrypt hashed password (nullable during migration, will be required after password reset flow)';
COMMENT ON TABLE  password_reset_tokens IS 'Stores single-use password reset tokens with a 1-hour expiration';
