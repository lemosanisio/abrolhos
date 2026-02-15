-- Migration: Prepare totp_secret column for encrypted storage
-- Requirements: 4.1, 4.5
-- This migration updates the totp_secret column to accommodate AES-256-GCM encrypted data
-- Format: Base64(12-byte-IV || encrypted-secret || 16-byte-auth-tag)

-- Alter totp_secret column to support longer encrypted values
-- Original: VARCHAR(255) for plaintext base32 TOTP secrets
-- Updated: VARCHAR(500) for Base64-encoded encrypted data with IV and auth tag
ALTER TABLE users ALTER COLUMN totp_secret TYPE VARCHAR(500);

-- Add column comment documenting the encryption format
COMMENT ON COLUMN users.totp_secret IS 'AES-256-GCM encrypted TOTP secret (Base64 encoded with IV and authentication tag). Format: Base64(12-byte-IV || ciphertext || 16-byte-tag)';
