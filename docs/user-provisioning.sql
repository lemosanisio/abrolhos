-- ============================================================================
-- User Provisioning Helper Scripts for TOTP Authentication
-- ============================================================================
-- These scripts help administrators manually provision users and generate
-- invitation tokens for the TOTP-only authentication system.
--
-- IMPORTANT: Replace placeholder values (username, role, expiry days) with
-- actual values before executing.
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Script 1: Create a new user and generate an invitation token
-- ----------------------------------------------------------------------------
-- This script creates an inactive user and generates a time-limited invitation
-- token that can be used to activate the account.
--
-- Parameters to customize:
--   - 'your_username': The username for the new user (3-20 chars, lowercase)
--   - 'ADMIN' or 'USER': The role for the user
--   - 7: Number of days until the invite expires (default: 7 days)
--
-- Usage:
--   1. Replace 'your_username' with the desired username
--   2. Replace 'ADMIN' with 'USER' if creating a regular user
--   3. Adjust expiry days if needed (default is 7 days)
--   4. Execute the script
--   5. Copy the invite_token from the output
--   6. Share the token with the user securely
-- ----------------------------------------------------------------------------

DO $$
DECLARE
    new_user_id CHAR(26);
    new_invite_id CHAR(26);
    new_invite_token VARCHAR(64);
    invite_expiry TIMESTAMPTZ;
BEGIN
    -- Generate ULID for user (timestamp-based sortable unique ID)
    new_user_id := (
        SELECT LPAD(TO_HEX(FLOOR(EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT), 10, '0') ||
               UPPER(SUBSTRING(MD5(RANDOM()::TEXT || CLOCK_TIMESTAMP()::TEXT) FROM 1 FOR 16))
    );
    
    -- Create inactive user
    INSERT INTO users (id, username, totp_secret, is_active, role, created_at, updated_at)
    VALUES (
        new_user_id,
        'your_username',  -- REPLACE WITH ACTUAL USERNAME
        NULL,             -- No TOTP secret yet (will be set during activation)
        FALSE,            -- User starts inactive
        'ADMIN',          -- REPLACE WITH 'USER' for regular users
        NOW(),
        NOW()
    );
    
    -- Generate ULID for invite
    new_invite_id := (
        SELECT LPAD(TO_HEX(FLOOR(EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT), 10, '0') ||
               UPPER(SUBSTRING(MD5(RANDOM()::TEXT || CLOCK_TIMESTAMP()::TEXT) FROM 1 FOR 16))
    );
    
    -- Generate secure random token (64 characters)
    new_invite_token := ENCODE(gen_random_bytes(32), 'hex');
    
    -- Calculate expiry date (7 days from now by default)
    invite_expiry := NOW() + INTERVAL '7 days';  -- ADJUST DAYS AS NEEDED
    
    -- Create invite
    INSERT INTO invites (id, token, user_id, expiry_date, created_at, updated_at)
    VALUES (
        new_invite_id,
        new_invite_token,
        new_user_id,
        invite_expiry,
        NOW(),
        NOW()
    );
    
    -- Output the results
    RAISE NOTICE '========================================';
    RAISE NOTICE 'User created successfully!';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'User ID: %', new_user_id;
    RAISE NOTICE 'Username: %', 'your_username';  -- REPLACE WITH ACTUAL USERNAME
    RAISE NOTICE 'Role: %', 'ADMIN';              -- REPLACE WITH ACTUAL ROLE
    RAISE NOTICE 'Status: INACTIVE (awaiting activation)';
    RAISE NOTICE '';
    RAISE NOTICE 'Invitation Details:';
    RAISE NOTICE '----------------------------------------';
    RAISE NOTICE 'Invite Token: %', new_invite_token;
    RAISE NOTICE 'Expires At: %', invite_expiry;
    RAISE NOTICE '';
    RAISE NOTICE 'IMPORTANT: Share this token securely with the user.';
    RAISE NOTICE 'The user will need this token to activate their account.';
    RAISE NOTICE '========================================';
END $$;


-- ----------------------------------------------------------------------------
-- Script 2: Retrieve an existing invite token for a user
-- ----------------------------------------------------------------------------
-- This script retrieves the invitation token for a specific user by username.
-- Useful when you need to resend an invitation or check if one exists.
--
-- Parameters to customize:
--   - 'username_here': The username to look up
--
-- Usage:
--   1. Replace 'username_here' with the actual username
--   2. Execute the script
--   3. The invite token and expiry date will be displayed
-- ----------------------------------------------------------------------------

DO $$
DECLARE
    user_id_var CHAR(26);
    invite_token_var VARCHAR(64);
    invite_expiry_var TIMESTAMPTZ;
    is_expired BOOLEAN;
BEGIN
    -- Find user by username
    SELECT id INTO user_id_var
    FROM users
    WHERE username = 'username_here'  -- REPLACE WITH ACTUAL USERNAME
    LIMIT 1;
    
    -- Check if user exists
    IF user_id_var IS NULL THEN
        RAISE NOTICE '========================================';
        RAISE NOTICE 'ERROR: User not found';
        RAISE NOTICE '========================================';
        RAISE NOTICE 'Username: %', 'username_here';  -- REPLACE WITH ACTUAL USERNAME
        RAISE NOTICE 'Please verify the username is correct.';
        RAISE NOTICE '========================================';
        RETURN;
    END IF;
    
    -- Find invite for user
    SELECT token, expiry_date INTO invite_token_var, invite_expiry_var
    FROM invites
    WHERE user_id = user_id_var
    AND deleted_at IS NULL
    LIMIT 1;
    
    -- Check if invite exists
    IF invite_token_var IS NULL THEN
        RAISE NOTICE '========================================';
        RAISE NOTICE 'No active invite found';
        RAISE NOTICE '========================================';
        RAISE NOTICE 'User ID: %', user_id_var;
        RAISE NOTICE 'Username: %', 'username_here';  -- REPLACE WITH ACTUAL USERNAME
        RAISE NOTICE '';
        RAISE NOTICE 'The user may have already activated their account,';
        RAISE NOTICE 'or the invite may have been deleted.';
        RAISE NOTICE '========================================';
        RETURN;
    END IF;
    
    -- Check if invite is expired
    is_expired := invite_expiry_var < NOW();
    
    -- Output the results
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Invite Token Retrieved';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'User ID: %', user_id_var;
    RAISE NOTICE 'Username: %', 'username_here';  -- REPLACE WITH ACTUAL USERNAME
    RAISE NOTICE '';
    RAISE NOTICE 'Invitation Details:';
    RAISE NOTICE '----------------------------------------';
    RAISE NOTICE 'Invite Token: %', invite_token_var;
    RAISE NOTICE 'Expires At: %', invite_expiry_var;
    RAISE NOTICE 'Status: %', CASE WHEN is_expired THEN 'EXPIRED' ELSE 'ACTIVE' END;
    RAISE NOTICE '';
    IF is_expired THEN
        RAISE NOTICE 'WARNING: This invite has expired!';
        RAISE NOTICE 'You may need to create a new invite for this user.';
    ELSE
        RAISE NOTICE 'This invite is still valid.';
        RAISE NOTICE 'Share this token securely with the user.';
    END IF;
    RAISE NOTICE '========================================';
END $$;


-- ----------------------------------------------------------------------------
-- Script 3: List all pending invites
-- ----------------------------------------------------------------------------
-- This script lists all active (non-expired, non-deleted) invites in the system.
-- Useful for auditing and monitoring pending user activations.
-- ----------------------------------------------------------------------------

SELECT 
    i.token AS invite_token,
    u.username,
    u.role,
    i.expiry_date,
    CASE 
        WHEN i.expiry_date < NOW() THEN 'EXPIRED'
        ELSE 'ACTIVE'
    END AS status,
    i.created_at AS invite_created_at
FROM invites i
JOIN users u ON i.user_id = u.id
WHERE i.deleted_at IS NULL
ORDER BY i.created_at DESC;


-- ----------------------------------------------------------------------------
-- Script 4: Delete an expired invite
-- ----------------------------------------------------------------------------
-- This script manually deletes an expired invite for a specific user.
-- The system automatically deletes expired invites during activation attempts,
-- but this script can be used for manual cleanup.
--
-- Parameters to customize:
--   - 'username_here': The username whose expired invite should be deleted
-- ----------------------------------------------------------------------------

DO $$
DECLARE
    user_id_var CHAR(26);
    deleted_count INTEGER;
BEGIN
    -- Find user by username
    SELECT id INTO user_id_var
    FROM users
    WHERE username = 'username_here'  -- REPLACE WITH ACTUAL USERNAME
    LIMIT 1;
    
    -- Check if user exists
    IF user_id_var IS NULL THEN
        RAISE NOTICE 'ERROR: User not found with username: %', 'username_here';
        RETURN;
    END IF;
    
    -- Delete expired invites for this user
    DELETE FROM invites
    WHERE user_id = user_id_var
    AND expiry_date < NOW()
    AND deleted_at IS NULL;
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    -- Output the results
    IF deleted_count > 0 THEN
        RAISE NOTICE 'Deleted % expired invite(s) for user: %', deleted_count, 'username_here';
    ELSE
        RAISE NOTICE 'No expired invites found for user: %', 'username_here';
    END IF;
END $$;


-- ----------------------------------------------------------------------------
-- Script 5: Create a new invite for an existing inactive user
-- ----------------------------------------------------------------------------
-- This script creates a new invitation token for an existing inactive user.
-- Useful when the original invite has expired or was lost.
--
-- Parameters to customize:
--   - 'username_here': The username to create a new invite for
--   - 7: Number of days until the invite expires (default: 7 days)
-- ----------------------------------------------------------------------------

DO $$
DECLARE
    user_id_var CHAR(26);
    user_active BOOLEAN;
    new_invite_id CHAR(26);
    new_invite_token VARCHAR(64);
    invite_expiry TIMESTAMPTZ;
BEGIN
    -- Find user by username
    SELECT id, is_active INTO user_id_var, user_active
    FROM users
    WHERE username = 'username_here'  -- REPLACE WITH ACTUAL USERNAME
    LIMIT 1;
    
    -- Check if user exists
    IF user_id_var IS NULL THEN
        RAISE NOTICE 'ERROR: User not found with username: %', 'username_here';
        RETURN;
    END IF;
    
    -- Check if user is already active
    IF user_active THEN
        RAISE NOTICE 'ERROR: User is already active. Cannot create invite for active user.';
        RAISE NOTICE 'Username: %', 'username_here';
        RETURN;
    END IF;
    
    -- Delete any existing invites for this user (expired or not)
    DELETE FROM invites
    WHERE user_id = user_id_var
    AND deleted_at IS NULL;
    
    -- Generate ULID for invite
    new_invite_id := (
        SELECT LPAD(TO_HEX(FLOOR(EXTRACT(EPOCH FROM NOW()) * 1000)::BIGINT), 10, '0') ||
               UPPER(SUBSTRING(MD5(RANDOM()::TEXT || CLOCK_TIMESTAMP()::TEXT) FROM 1 FOR 16))
    );
    
    -- Generate secure random token (64 characters)
    new_invite_token := ENCODE(gen_random_bytes(32), 'hex');
    
    -- Calculate expiry date (7 days from now by default)
    invite_expiry := NOW() + INTERVAL '7 days';  -- ADJUST DAYS AS NEEDED
    
    -- Create invite
    INSERT INTO invites (id, token, user_id, expiry_date, created_at, updated_at)
    VALUES (
        new_invite_id,
        new_invite_token,
        user_id_var,
        invite_expiry,
        NOW(),
        NOW()
    );
    
    -- Output the results
    RAISE NOTICE '========================================';
    RAISE NOTICE 'New invite created successfully!';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'User ID: %', user_id_var;
    RAISE NOTICE 'Username: %', 'username_here';  -- REPLACE WITH ACTUAL USERNAME
    RAISE NOTICE '';
    RAISE NOTICE 'Invitation Details:';
    RAISE NOTICE '----------------------------------------';
    RAISE NOTICE 'Invite Token: %', new_invite_token;
    RAISE NOTICE 'Expires At: %', invite_expiry;
    RAISE NOTICE '';
    RAISE NOTICE 'IMPORTANT: Share this token securely with the user.';
    RAISE NOTICE '========================================';
END $$;


-- ----------------------------------------------------------------------------
-- Script 6: Check user activation status
-- ----------------------------------------------------------------------------
-- This script checks the activation status of a user and displays relevant info.
--
-- Parameters to customize:
--   - 'username_here': The username to check
-- ----------------------------------------------------------------------------

DO $$
DECLARE
    user_record RECORD;
    invite_count INTEGER;
BEGIN
    -- Find user by username
    SELECT id, username, is_active, totp_secret, role, created_at
    INTO user_record
    FROM users
    WHERE username = 'username_here'  -- REPLACE WITH ACTUAL USERNAME
    LIMIT 1;
    
    -- Check if user exists
    IF user_record.id IS NULL THEN
        RAISE NOTICE 'ERROR: User not found with username: %', 'username_here';
        RETURN;
    END IF;
    
    -- Count active invites
    SELECT COUNT(*) INTO invite_count
    FROM invites
    WHERE user_id = user_record.id
    AND deleted_at IS NULL
    AND expiry_date > NOW();
    
    -- Output the results
    RAISE NOTICE '========================================';
    RAISE NOTICE 'User Status Report';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'User ID: %', user_record.id;
    RAISE NOTICE 'Username: %', user_record.username;
    RAISE NOTICE 'Role: %', user_record.role;
    RAISE NOTICE 'Created At: %', user_record.created_at;
    RAISE NOTICE '';
    RAISE NOTICE 'Activation Status:';
    RAISE NOTICE '----------------------------------------';
    RAISE NOTICE 'Is Active: %', user_record.is_active;
    RAISE NOTICE 'Has TOTP Secret: %', CASE WHEN user_record.totp_secret IS NOT NULL THEN 'YES' ELSE 'NO' END;
    RAISE NOTICE 'Active Invites: %', invite_count;
    RAISE NOTICE '';
    IF user_record.is_active THEN
        RAISE NOTICE 'Status: ACTIVE - User can log in with TOTP';
    ELSIF invite_count > 0 THEN
        RAISE NOTICE 'Status: PENDING - User has active invite(s)';
    ELSE
        RAISE NOTICE 'Status: INACTIVE - No active invites';
        RAISE NOTICE 'Action: Create a new invite for this user';
    END IF;
    RAISE NOTICE '========================================';
END $$;
