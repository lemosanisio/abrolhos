# Requirements Document: Password Authentication

## Introduction

This document specifies the requirements for adding password-based authentication to the Abrolhos backend application. The system currently uses TOTP-only authentication. This feature will introduce password authentication that works alongside TOTP, requiring both username + password + TOTP code for successful authentication.

The implementation will leverage the existing EncryptionService, audit logging, rate limiting, and security infrastructure already in place. All passwords will be securely hashed using bcrypt, and the system will enforce password policies, support password changes, and provide secure password reset flows.

## Glossary

- **Password_Service**: Service responsible for password hashing, verification, and validation
- **Auth_Service**: Existing service that orchestrates authentication flows
- **User**: Domain entity representing a user account
- **Password_Hash**: Bcrypt-hashed password stored in the database
- **Password_Reset_Token**: Cryptographically secure token used for password reset flows
- **Password_Policy**: Set of rules defining acceptable password requirements
- **Bcrypt**: Password hashing algorithm with configurable work factor
- **Work_Factor**: Bcrypt cost parameter controlling hash computation time (12-14 recommended)
- **Audit_Logger**: Existing service for logging security-relevant events
- **Rate_Limit_Service**: Existing service for preventing brute-force attacks
- **Encryption_Service**: Existing service for encrypting sensitive data using AES-256-GCM

## Requirements

### Requirement 1: Password Storage and Hashing

**User Story:** As a security engineer, I want passwords to be securely hashed using bcrypt, so that plaintext passwords are never stored and password data is protected against breaches.

#### Acceptance Criteria

1. THE Password_Service SHALL hash all passwords using bcrypt with a work factor between 12 and 14
2. THE User entity SHALL store the Password_Hash in a dedicated field
3. THE Password_Service SHALL never store or log plaintext passwords
4. WHEN a password is hashed, THE Password_Service SHALL generate a unique salt for each password
5. THE Password_Hash SHALL include the bcrypt version, work factor, salt, and hash in the standard bcrypt format

### Requirement 2: Password Validation and Policy Enforcement

**User Story:** As a security engineer, I want to enforce password complexity requirements, so that users create strong passwords that resist common attacks.

#### Acceptance Criteria

1. THE Password_Policy SHALL require passwords to be at least 12 characters in length
2. THE Password_Policy SHALL require passwords to contain at least one uppercase letter
3. THE Password_Policy SHALL require passwords to contain at least one lowercase letter
4. THE Password_Policy SHALL require passwords to contain at least one digit
5. THE Password_Policy SHALL require passwords to contain at least one special character from the set: !@#$%^&*()_+-=[]{}|;:,.<>?
6. THE Password_Policy SHALL reject passwords that exceed 128 characters in length
7. WHEN a password fails validation, THE Password_Service SHALL return a descriptive error message indicating which requirements were not met
8. THE Password_Service SHALL validate passwords before hashing them

### Requirement 3: Authentication with Password and TOTP

**User Story:** As a user, I want to authenticate using my username, password, and TOTP code, so that I can securely access the system with multi-factor authentication.

#### Acceptance Criteria

1. WHEN a user attempts to log in, THE Auth_Service SHALL require username, password, and TOTP code
2. WHEN all credentials are valid, THE Auth_Service SHALL generate and return a JWT token
3. IF the password is invalid, THEN THE Auth_Service SHALL reject the authentication attempt and return a generic error message
4. IF the TOTP code is invalid, THEN THE Auth_Service SHALL reject the authentication attempt and return a generic error message
5. THE Auth_Service SHALL verify the password before verifying the TOTP code
6. THE Auth_Service SHALL use constant-time comparison for password verification to prevent timing attacks
7. WHEN authentication fails, THE Auth_Service SHALL not reveal whether the username, password, or TOTP code was incorrect

### Requirement 4: Account Activation with Password

**User Story:** As a new user, I want to set my password during account activation, so that I can establish my credentials when first accessing the system.

#### Acceptance Criteria

1. WHEN a user activates their account, THE Auth_Service SHALL require an invitation token, password, and TOTP code
2. THE Auth_Service SHALL validate the password against the Password_Policy before activation
3. WHEN activation succeeds, THE Auth_Service SHALL store the Password_Hash in the User entity
4. WHEN activation succeeds, THE Auth_Service SHALL activate the account and return a JWT token
5. IF the password fails validation, THEN THE Auth_Service SHALL reject activation and return validation errors

### Requirement 5: Password Change for Authenticated Users

**User Story:** As an authenticated user, I want to change my password, so that I can update my credentials if they are compromised or I want to rotate them.

#### Acceptance Criteria

1. WHEN an authenticated user requests a password change, THE Password_Service SHALL require the current password and new password
2. THE Password_Service SHALL verify the current password before allowing the change
3. THE Password_Service SHALL validate the new password against the Password_Policy
4. THE Password_Service SHALL reject password changes where the new password matches the current password
5. WHEN a password change succeeds, THE Password_Service SHALL hash and store the new password
6. WHEN a password change succeeds, THE Audit_Logger SHALL log the password change event with user ID and timestamp

### Requirement 6: Password Reset Flow

**User Story:** As a user who has forgotten my password, I want to reset my password using a secure token, so that I can regain access to my account.

#### Acceptance Criteria

1. WHEN a password reset is requested, THE Password_Service SHALL generate a cryptographically secure Password_Reset_Token with 256 bits of entropy
2. THE Password_Reset_Token SHALL expire after 1 hour
3. THE Password_Service SHALL store the Password_Reset_Token with the user ID and expiration timestamp
4. WHEN a user submits a Password_Reset_Token and new password, THE Password_Service SHALL validate the token is not expired
5. WHEN a user submits a Password_Reset_Token and new password, THE Password_Service SHALL validate the new password against the Password_Policy
6. WHEN password reset succeeds, THE Password_Service SHALL invalidate the Password_Reset_Token
7. WHEN password reset succeeds, THE Audit_Logger SHALL log the password reset event with user ID and timestamp
8. THE Password_Service SHALL delete expired Password_Reset_Token entries

### Requirement 7: Rate Limiting for Password Operations

**User Story:** As a security engineer, I want to rate limit password-related operations, so that brute-force attacks are prevented.

#### Acceptance Criteria

1. WHEN a user attempts authentication, THE Rate_Limit_Service SHALL enforce a limit of 5 attempts per username per 15-minute window
2. WHEN the rate limit is exceeded, THE Auth_Service SHALL reject the authentication attempt with a rate limit error
3. WHEN a user attempts password reset, THE Rate_Limit_Service SHALL enforce a limit of 3 reset requests per username per 1-hour window
4. WHEN the rate limit is exceeded, THE Password_Service SHALL reject the reset request with a rate limit error
5. THE Rate_Limit_Service SHALL use the existing Redis-based rate limiting infrastructure

### Requirement 8: Audit Logging for Password Events

**User Story:** As a security engineer, I want all password-related events to be logged, so that I can monitor for suspicious activity and investigate security incidents.

#### Acceptance Criteria

1. WHEN a password is changed, THE Audit_Logger SHALL log an event with user ID, timestamp, and IP address
2. WHEN a password reset is requested, THE Audit_Logger SHALL log an event with username, timestamp, and IP address
3. WHEN a password reset is completed, THE Audit_Logger SHALL log an event with user ID, timestamp, and IP address
4. WHEN authentication fails due to invalid password, THE Audit_Logger SHALL log an event with username, timestamp, and IP address
5. WHEN rate limiting blocks a password operation, THE Audit_Logger SHALL log an event with username, timestamp, and operation type
6. THE Audit_Logger SHALL use the existing audit logging infrastructure

### Requirement 9: Password Verification Performance

**User Story:** As a system administrator, I want password verification to complete within acceptable time limits, so that authentication remains responsive while maintaining security.

#### Acceptance Criteria

1. THE Password_Service SHALL complete password hashing operations within 500 milliseconds at the 95th percentile
2. THE Password_Service SHALL complete password verification operations within 500 milliseconds at the 95th percentile
3. WHEN password operations exceed performance thresholds, THE Password_Service SHALL log a warning with operation duration
4. THE Password_Service SHALL expose metrics for password operation duration using Micrometer

### Requirement 10: Database Schema for Password Data

**User Story:** As a developer, I want the database schema to support password storage and reset tokens, so that password data is properly persisted.

#### Acceptance Criteria

1. THE User table SHALL include a password_hash column of type VARCHAR(60) to store bcrypt hashes
2. THE User table SHALL allow password_hash to be nullable during the migration period
3. THE System SHALL create a password_reset_tokens table with columns: id, user_id, token, expires_at, created_at
4. THE password_reset_tokens table SHALL have a foreign key constraint to the users table
5. THE password_reset_tokens table SHALL have an index on the token column for efficient lookups
6. THE password_reset_tokens table SHALL have an index on the expires_at column for efficient cleanup queries

### Requirement 11: Password Service Interface

**User Story:** As a developer, I want a clean service interface for password operations, so that password functionality is easy to use and maintain.

#### Acceptance Criteria

1. THE Password_Service SHALL provide a method to hash passwords that returns a Password_Hash
2. THE Password_Service SHALL provide a method to verify passwords that accepts plaintext and Password_Hash
3. THE Password_Service SHALL provide a method to validate passwords against the Password_Policy
4. THE Password_Service SHALL provide a method to generate Password_Reset_Token entries
5. THE Password_Service SHALL provide a method to validate and consume Password_Reset_Token entries
6. THE Password_Service SHALL provide a method to change passwords for authenticated users
7. THE Password_Service SHALL follow the existing service patterns in the application (dependency injection, transaction management)

### Requirement 12: Integration with Existing Authentication Flow

**User Story:** As a developer, I want password authentication to integrate seamlessly with the existing TOTP authentication, so that the system maintains its security posture.

#### Acceptance Criteria

1. THE Auth_Service SHALL continue to require TOTP codes for all authentication attempts
2. THE Auth_Service SHALL verify passwords before TOTP codes to fail fast on invalid passwords
3. THE Auth_Service SHALL maintain the existing JWT token generation logic
4. THE Auth_Service SHALL maintain the existing user activation flow with the addition of password setting
5. WHEN a user without a password attempts to log in, THE Auth_Service SHALL reject the authentication attempt with a message directing them to use the password reset flow
