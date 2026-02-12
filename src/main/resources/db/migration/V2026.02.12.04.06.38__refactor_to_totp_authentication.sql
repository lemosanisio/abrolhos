-- Enable pgcrypto extension for secure random generation
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Drop old columns from users table (if they exist)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'users' AND column_name = 'email') THEN
        ALTER TABLE users DROP COLUMN email;
    END IF;
    
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'users' AND column_name = 'password_hash') THEN
        ALTER TABLE users DROP COLUMN password_hash;
    END IF;
END $$;

-- Add new columns to users table (if they don't exist)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'users' AND column_name = 'totp_secret') THEN
        ALTER TABLE users ADD COLUMN totp_secret VARCHAR(255);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'users' AND column_name = 'is_active') THEN
        ALTER TABLE users ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END $$;

-- Set all existing users to inactive with no TOTP secret (idempotent)
UPDATE users SET is_active = FALSE, totp_secret = NULL 
WHERE is_active = TRUE OR totp_secret IS NOT NULL;

-- Create invites table (if it doesn't exist)
CREATE TABLE IF NOT EXISTS invites (
    id CHAR(26) PRIMARY KEY,
    token VARCHAR(64) NOT NULL,
    user_id CHAR(26) NOT NULL,
    expiry_date TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ DEFAULT NULL
);

-- Add unique constraint on token (if it doesn't exist)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint 
                   WHERE conname = 'invites_token_key') THEN
        ALTER TABLE invites ADD CONSTRAINT invites_token_key UNIQUE (token);
    END IF;
END $$;

-- Add foreign key constraint (if it doesn't exist)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint 
                   WHERE conname = 'invites_user_id_fkey') THEN
        ALTER TABLE invites ADD CONSTRAINT invites_user_id_fkey 
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

-- Create indexes for performance (if they don't exist)
CREATE INDEX IF NOT EXISTS idx_invites_token ON invites(token);
CREATE INDEX IF NOT EXISTS idx_invites_user_id ON invites(user_id);
CREATE INDEX IF NOT EXISTS idx_invites_expiry_date ON invites(expiry_date);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active);
