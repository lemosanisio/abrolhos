# User Provisioning Quick Reference

This guide provides quick commands for common user provisioning tasks in the TOTP authentication system.

## Prerequisites

- Access to the PostgreSQL database
- Database client (psql, DBeaver, pgAdmin, etc.)
- The `user-provisioning.sql` file

## Common Tasks

### 1. Create a New User with Invite

**File:** `docs/user-provisioning.sql` → Script 1

**Steps:**
1. Replace `'your_username'` with the desired username
2. Replace `'ADMIN'` with `'USER'` for regular users
3. Adjust expiry days if needed (default: 7)
4. Execute the script
5. Copy the `invite_token` from the output
6. Share the token securely with the user

**Output Example:**
```
========================================
User created successfully!
========================================
User ID: 01JFA123456789ABCDEFGHIJK
Username: johndoe
Role: USER
Status: INACTIVE (awaiting activation)

Invitation Details:
----------------------------------------
Invite Token: a1b2c3d4e5f6...
Expires At: 2025-01-22 14:30:00+00
========================================
```

### 2. Retrieve Existing Invite Token

**File:** `docs/user-provisioning.sql` → Script 2

**Steps:**
1. Replace `'username_here'` with the actual username
2. Execute the script
3. Copy the token from the output

**Use Cases:**
- User lost their invitation email
- Need to resend the invitation
- Verify if an invite exists

### 3. List All Pending Invites

**File:** `docs/user-provisioning.sql` → Script 3

**Steps:**
1. Execute the script as-is (no modifications needed)
2. Review the list of pending invites

**Output Columns:**
- `invite_token`: The invitation token
- `username`: Username of the invited user
- `role`: User role (ADMIN or USER)
- `expiry_date`: When the invite expires
- `status`: ACTIVE or EXPIRED
- `invite_created_at`: When the invite was created

### 4. Create New Invite for Existing User

**File:** `docs/user-provisioning.sql` → Script 5

**Steps:**
1. Replace `'username_here'` with the actual username
2. Adjust expiry days if needed (default: 7)
3. Execute the script
4. Copy the new `invite_token` from the output

**Use Cases:**
- Original invite expired
- User lost their invitation token
- Need to regenerate an invite

**Note:** This script automatically deletes any existing invites for the user.

### 5. Check User Status

**File:** `docs/user-provisioning.sql` → Script 6

**Steps:**
1. Replace `'username_here'` with the actual username
2. Execute the script
3. Review the user's activation status

**Output Information:**
- User ID and username
- Role and creation date
- Activation status (ACTIVE/PENDING/INACTIVE)
- Whether user has TOTP secret configured
- Number of active invites

### 6. Delete Expired Invite

**File:** `docs/user-provisioning.sql` → Script 4

**Steps:**
1. Replace `'username_here'` with the actual username
2. Execute the script

**Note:** The system automatically deletes expired invites during activation attempts, so manual cleanup is rarely needed.

## User Activation Process

### For Administrators

1. Create user and invite using Script 1
2. Share the invite token with the user securely (email, secure messaging, etc.)
3. Provide instructions for activation (see below)

### For Users

1. Receive invitation token from administrator
2. Open authenticator app (Google Authenticator, Authy, 1Password, etc.)
3. Add new account using the TOTP secret or QR code
4. Generate a 6-digit TOTP code from the app
5. Call the activation endpoint:
   ```bash
   POST /api/auth/activate
   {
     "inviteToken": "a1b2c3d4e5f6...",
     "totpCode": "123456"
   }
   ```
6. Save the returned JWT token
7. Use the token for subsequent API requests

### Login After Activation

Once activated, users log in with their username and current TOTP code:

```bash
POST /api/auth/login
{
  "username": "johndoe",
  "totpCode": "123456"
}
```

## Troubleshooting

### "Invalid or expired invite token"

**Possible Causes:**
- Invite has expired (check expiry date)
- Invite was already used
- Token was typed incorrectly

**Solutions:**
- Use Script 2 to check invite status
- Use Script 5 to create a new invite if expired
- Verify token is copied correctly (no extra spaces)

### "Account is already active"

**Cause:** User has already activated their account

**Solution:** User should use the login endpoint instead of activation

### "Invalid TOTP code"

**Possible Causes:**
- Code expired (TOTP codes are valid for 30 seconds)
- Clock skew between server and user device
- Wrong secret configured in authenticator app

**Solutions:**
- Generate a fresh TOTP code
- Ensure device time is synchronized (automatic time zone)
- Verify the correct secret was added to authenticator app
- Try again with a new code

### "User not found"

**Cause:** Username doesn't exist in the database

**Solution:**
- Verify username spelling
- Use Script 6 to check if user exists
- Create user using Script 1 if needed

## Security Best Practices

1. **Token Sharing**: Share invite tokens through secure channels (encrypted email, secure messaging apps)
2. **Expiry Period**: Use shorter expiry periods (3-7 days) for sensitive environments
3. **Token Storage**: Don't store invite tokens in plain text logs or unsecured locations
4. **Cleanup**: Periodically review and clean up expired invites using Script 3 and Script 4
5. **Monitoring**: Monitor failed activation attempts for potential security issues
6. **Time Sync**: Ensure server and user devices have accurate time synchronization for TOTP

## Database Queries

### Quick Queries for Manual Inspection

```sql
-- Check if user exists
SELECT id, username, is_active, role, created_at
FROM users
WHERE username = 'johndoe';

-- Check active invites for a user
SELECT i.token, i.expiry_date, i.created_at,
       CASE WHEN i.expiry_date < NOW() THEN 'EXPIRED' ELSE 'ACTIVE' END AS status
FROM invites i
JOIN users u ON i.user_id = u.id
WHERE u.username = 'johndoe'
AND i.deleted_at IS NULL;

-- Count users by status
SELECT 
    is_active,
    COUNT(*) as user_count
FROM users
WHERE deleted_at IS NULL
GROUP BY is_active;

-- Find users with expired invites
SELECT u.username, i.expiry_date
FROM users u
JOIN invites i ON u.id = i.user_id
WHERE u.is_active = FALSE
AND i.expiry_date < NOW()
AND i.deleted_at IS NULL;
```

## API Endpoints

### Activation Endpoint

```
POST /api/auth/activate
Content-Type: application/json

{
  "inviteToken": "string (64 hex characters)",
  "totpCode": "string (6 digits)"
}

Response 200 OK:
{
  "token": "JWT token string"
}

Response 400 Bad Request:
{
  "error": "Invalid or expired invite token"
}
```

### Login Endpoint

```
POST /api/auth/login
Content-Type: application/json

{
  "username": "string",
  "totpCode": "string (6 digits)"
}

Response 200 OK:
{
  "token": "JWT token string"
}

Response 401 Unauthorized:
{
  "error": "Invalid credentials"
}
```

## Notes

- All timestamps are in UTC
- ULID format: 26 characters (timestamp + randomness)
- Invite tokens: 64 hex characters (256 bits of entropy)
- TOTP codes: 6 digits, valid for 30 seconds with ±30s window
- JWT tokens: Configurable expiry (default: 24 hours)
