-- Add totp_secret column to invites table for secret persistence across page refreshes
ALTER TABLE invites ADD COLUMN IF NOT EXISTS totp_secret VARCHAR(255);
