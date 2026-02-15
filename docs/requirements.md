# Requirements Document

## Introduction

This document specifies the security hardening requirements for the Abrolhos application, a Kotlin/Spring Boot backend with React frontend that uses TOTP-based authentication. The system currently has several security vulnerabilities that need to be addressed: unrestricted CORS configuration, unenforced rate limiting, plaintext TOTP secret storage, and missing audit logging for sensitive operations.

The security hardening improvements will strengthen the application's security posture while maintaining backward compatibility where possible and ensuring all changes are thoroughly tested using both property-based and unit testing approaches.

## Glossary

- **CORS_Manager**: The component responsible for managing Cross-Origin Resource Sharing configuration
- **Rate_Limiter**: The component that enforces request rate limits on authentication endpoints
- **Secret_Encryptor**: The component that encrypts and decrypts TOTP secrets using AES-256-GCM
- **Audit_Logger**: The component that records security-relevant events to an audit trail
- **Auth_Endpoint**: Any HTTP endpoint under /api/auth/ that handles authentication operations
- **TOTP_Secret**: The base32-encoded secret used for Time-based One-Time Password generation
- **Encrypted_Secret**: A TOTP secret that has been encrypted using AES-256-GCM with an initialization vector
- **Audit_Event**: A structured log entry recording a security-relevant operation with timestamp, user context, and outcome
- **Configuration_Source**: Environment variables or application.yml properties that configure the system
- **Migration_Script**: A database migration that transforms existing data to a new schema

## Requirements

### Requirement 1: CORS Configuration Hardening

**User Story:** As a security administrator, I want CORS to be restricted to specific allowed origins, so that unauthorized domains cannot make cross-origin requests to the API.

#### Acceptance Criteria

1. WHEN the application starts, THE CORS_Manager SHALL load allowed origins from the Configuration_Source
2. WHEN a cross-origin request is received, THE CORS_Manager SHALL validate the origin against the allowed origins list
3. IF the origin is not in the allowed list, THEN THE CORS_Manager SHALL reject the request with appropriate CORS headers
4. WHERE the Configuration_Source specifies multiple origins, THE CORS_Manager SHALL accept requests from any origin in the list
5. WHEN no allowed origins are configured, THE CORS_Manager SHALL reject all cross-origin requests
6. THE CORS_Manager SHALL continue to allow the HTTP methods GET, POST, PUT, DELETE, OPTIONS, and PATCH
7. THE CORS_Manager SHALL continue to allow all standard headers required by the frontend

### Requirement 2: Rate Limiting Enforcement

**User Story:** As a security administrator, I want rate limiting enforced on authentication endpoints, so that brute force attacks and credential stuffing are mitigated.

#### Acceptance Criteria

1. WHEN the application starts, THE Rate_Limiter SHALL load rate limit configuration from the Configuration_Source
2. WHEN a request is made to an Auth_Endpoint, THE Rate_Limiter SHALL track the request count per client identifier
3. IF a client exceeds the maximum attempts within the time window, THEN THE Rate_Limiter SHALL reject subsequent requests with HTTP 429 status
4. WHEN the time window expires, THE Rate_Limiter SHALL reset the request count for that client
5. THE Rate_Limiter SHALL identify clients by IP address for unauthenticated requests
6. THE Rate_Limiter SHALL apply rate limits to the /api/auth/login endpoint
7. THE Rate_Limiter SHALL apply rate limits to the /api/auth/activate endpoint
8. THE Rate_Limiter SHALL apply rate limits to the /api/auth/invite/* endpoints
9. WHEN a rate limit is exceeded, THE Rate_Limiter SHALL include a Retry-After header indicating when the client can retry

### Requirement 3: TOTP Secret Encryption

**User Story:** As a security administrator, I want TOTP secrets encrypted at rest in the database, so that compromised database backups do not expose authentication secrets.

#### Acceptance Criteria

1. WHEN a TOTP_Secret is stored, THE Secret_Encryptor SHALL encrypt it using AES-256-GCM before database persistence
2. WHEN a TOTP_Secret is retrieved, THE Secret_Encryptor SHALL decrypt it before returning to the application
3. THE Secret_Encryptor SHALL generate a unique initialization vector for each encryption operation
4. THE Secret_Encryptor SHALL store the initialization vector alongside the encrypted secret
5. THE Secret_Encryptor SHALL load the encryption key from the Configuration_Source
6. WHEN encryption fails, THE Secret_Encryptor SHALL throw an exception and prevent the operation from completing
7. WHEN decryption fails, THE Secret_Encryptor SHALL throw an exception and prevent authentication from proceeding
8. FOR ALL valid TOTP_Secret values, encrypting then decrypting SHALL produce an equivalent secret (round-trip property)
9. THE Secret_Encryptor SHALL use authenticated encryption to detect tampering

### Requirement 4: Database Migration for Encrypted Secrets

**User Story:** As a system administrator, I want existing plaintext TOTP secrets migrated to encrypted format, so that all secrets are protected without requiring user re-enrollment.

#### Acceptance Criteria

1. WHEN the Migration_Script executes, THE system SHALL encrypt all existing plaintext TOTP secrets
2. WHEN the Migration_Script completes, THE system SHALL verify all secrets were successfully encrypted
3. IF any encryption operation fails during migration, THEN THE Migration_Script SHALL roll back all changes
4. THE Migration_Script SHALL preserve the original secret values through the encryption process
5. THE Migration_Script SHALL update the database schema to accommodate initialization vectors
6. WHEN the migration is complete, THE system SHALL be able to authenticate users with their existing TOTP codes

### Requirement 5: Audit Logging for Authentication Events

**User Story:** As a security administrator, I want comprehensive audit logs for authentication events, so that I can investigate security incidents and detect suspicious activity.

#### Acceptance Criteria

1. WHEN a user attempts to log in, THE Audit_Logger SHALL record an Audit_Event with username, timestamp, IP address, and outcome
2. WHEN a user activates their account, THE Audit_Logger SHALL record an Audit_Event with username, timestamp, and invitation token used
3. WHEN an invitation token is used, THE Audit_Logger SHALL record an Audit_Event with token identifier, timestamp, and outcome
4. WHEN a rate limit is exceeded, THE Audit_Logger SHALL record an Audit_Event with client identifier, endpoint, and timestamp
5. WHEN authentication fails, THE Audit_Logger SHALL record the failure reason in the Audit_Event
6. THE Audit_Logger SHALL write Audit_Events to a structured log format suitable for log aggregation systems
7. THE Audit_Logger SHALL include correlation IDs to trace related events across multiple log entries
8. THE Audit_Logger SHALL NOT log sensitive data such as TOTP codes or encryption keys

### Requirement 6: Configuration Management

**User Story:** As a system administrator, I want security settings configurable via environment variables, so that I can adjust security parameters without code changes.

#### Acceptance Criteria

1. THE Configuration_Source SHALL provide a property for allowed CORS origins as a comma-separated list
2. THE Configuration_Source SHALL provide properties for rate limit maximum attempts and time window
3. THE Configuration_Source SHALL provide a property for the encryption key used by Secret_Encryptor
4. WHEN a required security configuration property is missing, THE system SHALL fail to start with a clear error message
5. WHEN configuration values are invalid, THE system SHALL fail to start with a descriptive validation error
6. THE system SHALL provide sensible default values for non-critical security settings

### Requirement 7: Backward Compatibility

**User Story:** As a developer, I want security improvements to maintain API compatibility, so that existing clients continue to function without modification.

#### Acceptance Criteria

1. WHEN security hardening is deployed, THE system SHALL maintain the same API endpoints and request/response formats
2. WHEN TOTP secrets are encrypted, THE system SHALL continue to accept the same TOTP codes from users
3. WHEN rate limiting is enforced, THE system SHALL return standard HTTP status codes that clients can handle
4. THE system SHALL continue to support the existing JWT token format and validation logic
5. THE system SHALL continue to support the existing invitation token workflow

### Requirement 8: Testing and Validation

**User Story:** As a developer, I want comprehensive automated tests for security features, so that I can verify correctness and prevent regressions.

#### Acceptance Criteria

1. FOR ALL valid TOTP secrets, THE encryption round-trip property SHALL be verified by property-based tests
2. FOR ALL rate limit configurations, THE system SHALL verify that limits are enforced correctly
3. FOR ALL CORS configurations, THE system SHALL verify that only allowed origins are accepted
4. THE test suite SHALL include property-based tests that generate random encryption inputs
5. THE test suite SHALL include unit tests for specific edge cases such as empty secrets or invalid keys
6. THE test suite SHALL include integration tests that verify audit events are logged correctly
7. WHEN tests generate encrypted data, THE tests SHALL verify that decryption produces the original value
