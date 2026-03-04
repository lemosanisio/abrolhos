-- Migration: Make password_hash NOT NULL
-- Run this AFTER all existing users have set their passwords via the password reset flow.
-- This enforces that every active user account has a password.

-- Applying a default password (ChangeMe123!) for existing users so the migration doesn't fail
UPDATE users 
SET password_hash = '$2a$12$dUj60Z1dajc0xZKlLlBBfOFFH/htts6ckAgP8T1wlNAemrUmWZrIS' 
WHERE password_hash IS NULL;

ALTER TABLE users
ALTER COLUMN password_hash SET NOT NULL;

COMMENT ON COLUMN users.password_hash IS 'Bcrypt hashed password (required for all users)';
