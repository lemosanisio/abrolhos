# Implementation Plan: Password Authentication

## Overview

This implementation plan breaks down the password authentication feature into discrete, incremental coding tasks. Each task builds on previous tasks and includes references to specific requirements. The implementation follows the hexagonal architecture pattern with Domain-Driven Design principles.

## Tasks

- [ ] 1. Create domain layer value objects and entities
  - [ ] 1.1 Create PlaintextPassword value class
    - Implement inline value class with validation (not blank, max 128 chars)
    - _Requirements: 2.6_
  
  - [ ] 1.2 Create PasswordHash value class
    - Implement inline value class with bcrypt format validation
    - Validate hash starts with $2a$, $2b$, or $2y$
    - _Requirements: 1.5_
  
  - [ ] 1.3 Create PasswordResetToken value class
    - Implement inline value class with 64-character hex validation
    - _Requirements: 6.1_
  
  - [ ] 1.4 Extend User domain entity
    - Add nullable passwordHash: PasswordHash? field
    - Update constructor and copy method
    - _Requirements: 10.1, 10.2_
  
  - [ ] 1.5 Create PasswordResetTokenEntity domain entity
    - Implement data class with id, userId, token, expiresAt, createdAt
    - Add isExpired() method
    - _Requirements: 6.2, 6.3_
  
  - [ ] 1.6 Create password exception hierarchy
    - Create sealed class PasswordException
    - Create InvalidPasswordException
    - Create PasswordResetException
    - Create PasswordPolicyViolationException with violations list
    - Create PasswordResetTokenExpiredException
    - Create PasswordResetTokenNotFoundException
    - Create RateLimitExceededException
    - _Requirements: 2.7_

- [ ] 2. Create repository interfaces and implementations
  - [ ] 2.1 Create PasswordResetTokenRepository interface
    - Define save, findByToken, deleteById, deleteExpiredTokens, deleteByUserId methods
    - _Requirements: 6.3, 6.8_
  
  - [ ] 2.2 Create PasswordResetTokenEntity JPA entity
    - Implement entity with proper annotations
    - Add indexes on token and expires_at columns
    - Add foreign key constraint to users table
    - _Requirements: 10.3, 10.4, 10.5, 10.6_
  
  - [ ] 2.3 Create PasswordResetTokenJpaRepository interface
    - Extend JpaRepository
    - Add custom query methods: findByToken, deleteByExpiresAtBefore, deleteByUserId
    - _Requirements: 6.3, 6.8_
  
  - [ ] 2.4 Create PasswordResetTokenRepositoryImpl
    - Implement domain repository interface
    - Map between JPA entities and domain entities
    - _Requirements: 6.3_

- [ ] 3. Create configuration and properties
  - [ ] 3.1 Create PasswordProperties configuration class
    - Implement @ConfigurationProperties data class
    - Add properties: minLength, maxLength, requireUppercase, requireLowercase, requireDigit, requireSpecialChar, bcryptStrength, specialChars, resetTokenExpiryHours, resetTokenByteSize
    - Set default values matching requirements
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 6.1, 6.2_
  
  - [ ] 3.2 Create PasswordConfiguration class
    - Implement @Configuration with @EnableConfigurationProperties
    - Create PasswordEncoder bean using BCryptPasswordEncoder with configured strength
    - Create SecureRandom bean with @ConditionalOnMissingBean
    - _Requirements: 1.1, 11.7_
  
  - [ ] 3.3 Add password configuration to application.yml
    - Add security.password section with all properties
    - Add cleanup-cron configuration
    - _Requirements: 1.1, 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

- [ ] 4. Implement PasswordService
  - [ ] 4.1 Create PasswordService class skeleton
    - Add @Service annotation
    - Inject dependencies: PasswordEncoder, PasswordResetTokenRepository, AuditLogger, RateLimitService, MeterRegistry, PasswordProperties, SecureRandom
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6_
  
  - [ ] 4.2 Implement validatePassword method
    - Check minimum length (12 chars)
    - Check maximum length (128 chars)
    - Check for uppercase letter
    - Check for lowercase letter
    - Check for digit
    - Check for special character
    - Return list of violation messages
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7_
  
  - [ ]* 4.3 Write property test for password validation
    - **Property 2: Password Policy Validation**
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8**
  
  - [ ] 4.4 Implement hashPassword method
    - Validate password before hashing
    - Use PasswordEncoder.encode()
    - Record metrics for operation duration
    - Log warning if operation exceeds 500ms threshold
    - Return PasswordHash
    - _Requirements: 1.1, 1.4, 2.8, 9.1, 9.3, 9.4_
  
  - [ ]* 4.5 Write property test for bcrypt hash format and uniqueness
    - **Property 1: Bcrypt Hash Format and Uniqueness**
    - **Validates: Requirements 1.1, 1.4, 1.5**
  
  - [ ] 4.6 Implement verifyPassword method
    - Use PasswordEncoder.matches() for constant-time comparison
    - Record metrics for operation duration
    - Log warning if operation exceeds 500ms threshold
    - _Requirements: 3.6, 9.2, 9.3, 9.4_
  
  - [ ]* 4.7 Write property test for password verification round-trip
    - **Property 3: Password Verification Round-Trip**
    - **Validates: Requirements 1.1, 3.2**
  
  - [ ] 4.8 Implement changePassword method
    - Check rate limit for user
    - Verify current password matches current hash
    - Validate new password against policy
    - Reject if new password equals current password
    - Hash new password
    - Log audit event with user ID, timestamp, IP address
    - Record metrics
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 8.1_
  
  - [ ]* 4.9 Write property test for password change
    - **Property 7: Password Change Requires Current Password**
    - **Validates: Requirements 5.2, 5.3, 5.4**
  
  - [ ]* 4.10 Write property test for password change persistence
    - **Property 8: Password Change Persistence and Audit**
    - **Validates: Requirements 5.5, 5.6**
  
  - [ ] 4.11 Implement generateResetToken method
    - Check rate limit for username (3 per hour)
    - Generate cryptographically secure 64-char hex token using SecureRandom
    - Calculate expiration time (1 hour from now)
    - Delete any existing tokens for user
    - Save token to repository
    - Log audit event with username, timestamp, IP address
    - Record metrics
    - _Requirements: 6.1, 6.2, 6.3, 7.3, 7.4, 8.2_
  
  - [ ]* 4.12 Write property test for reset token generation
    - **Property 9: Password Reset Token Generation**
    - **Validates: Requirements 6.1, 6.2, 6.3**
  
  - [ ] 4.13 Implement resetPassword method
    - Find token in repository
    - Validate token is not expired
    - Validate new password against policy
    - Hash new password
    - Update user's password hash
    - Delete token from repository
    - Log audit event with user ID, timestamp, IP address
    - Record metrics
    - _Requirements: 6.4, 6.5, 6.6, 6.7, 8.3_
  
  - [ ]* 4.14 Write property test for reset token single-use
    - **Property 10: Password Reset Token Single-Use**
    - **Validates: Requirements 6.4, 6.6**
  
  - [ ] 4.15 Implement cleanupExpiredTokens method
    - Add @Scheduled annotation with configurable cron
    - Call repository.deleteExpiredTokens()
    - Log number of deleted tokens
    - Record metrics
    - _Requirements: 6.8_
  
  - [ ]* 4.16 Write property test for expired token cleanup
    - **Property 12: Expired Token Cleanup**
    - **Validates: Requirements 6.8**

- [ ] 5. Checkpoint - Ensure PasswordService tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 6. Extend AuthService for password authentication
  - [ ] 6.1 Update AuthService constructor
    - Add PasswordService dependency
    - _Requirements: 12.1_
  
  - [ ] 6.2 Update login method signature
    - Add password: PlaintextPassword parameter
    - _Requirements: 3.1_
  
  - [ ] 6.3 Implement password verification in login
    - Check rate limit (5 attempts per 15 minutes)
    - Check if user has password set (null check)
    - Verify password before TOTP verification
    - Return generic error message on any failure
    - Log audit event on password failure
    - _Requirements: 3.1, 3.2, 3.3, 3.5, 3.7, 7.1, 7.2, 8.4, 12.2_
  
  - [ ]* 6.4 Write property test for authentication generic errors
    - **Property 4: Authentication Generic Error Messages**
    - **Validates: Requirements 3.3, 3.4, 3.7**
  
  - [ ]* 6.5 Write property test for authentication requires all credentials
    - **Property 5: Authentication Requires All Credentials**
    - **Validates: Requirements 3.1, 3.2, 12.1**
  
  - [ ]* 6.6 Write property test for password required
    - **Property 17: Password Required for Authentication**
    - **Validates: Requirements 3.1, 12.1**
  
  - [ ] 6.7 Update activateAccount method signature
    - Add password: PlaintextPassword parameter
    - _Requirements: 4.1_
  
  - [ ] 6.8 Implement password setting in activateAccount
    - Validate password against policy
    - Hash password
    - Store password hash in user entity
    - Return validation errors if password invalid
    - _Requirements: 4.2, 4.3, 4.5_
  
  - [ ]* 6.9 Write property test for account activation
    - **Property 6: Account Activation Sets Password**
    - **Validates: Requirements 4.2, 4.3, 4.4, 12.4**

- [ ] 7. Create web layer DTOs
  - [ ] 7.1 Update LoginRequest DTO
    - Add password: String field with @NotBlank validation
    - _Requirements: 3.1_
  
  - [ ] 7.2 Update ActivateAccountRequest DTO
    - Add password: String field with @NotBlank validation
    - _Requirements: 4.1_
  
  - [ ] 7.3 Create ChangePasswordRequest DTO
    - Add currentPassword and newPassword fields with @NotBlank validation
    - _Requirements: 5.1_
  
  - [ ] 7.4 Create PasswordResetRequest DTO
    - Add username field with @NotBlank validation
    - _Requirements: 6.1_
  
  - [ ] 7.5 Create ConfirmPasswordResetRequest DTO
    - Add token field with @NotBlank and @Size(64) validation
    - Add newPassword field with @NotBlank validation
    - _Requirements: 6.4_
  
  - [ ] 7.6 Create PasswordValidationErrorResponse DTO
    - Add violations: List<String> field
    - _Requirements: 2.7_

- [ ] 8. Create PasswordController
  - [ ] 8.1 Create PasswordController class skeleton
    - Add @RestController, @RequestMapping("/api/password"), @Validated annotations
    - Inject PasswordService and UserRepository
    - _Requirements: 11.6_
  
  - [ ] 8.2 Implement changePassword endpoint
    - Add @PostMapping("/change") and @PreAuthorize("isAuthenticated()")
    - Extract user ID from @AuthenticationPrincipal
    - Call passwordService.changePassword()
    - Handle PasswordPolicyViolationException and return 400 with violations
    - Handle InvalidPasswordException and return 401
    - Handle RateLimitExceededException and return 429
    - Return 204 No Content on success
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_
  
  - [ ] 8.3 Implement requestPasswordReset endpoint
    - Add @PostMapping("/reset/request")
    - Call passwordService.generateResetToken()
    - Always return 202 Accepted to prevent user enumeration
    - Handle RateLimitExceededException and return 429
    - _Requirements: 6.1, 6.2, 6.3, 7.3, 7.4_
  
  - [ ] 8.4 Implement confirmPasswordReset endpoint
    - Add @PostMapping("/reset/confirm")
    - Call passwordService.resetPassword()
    - Handle PasswordPolicyViolationException and return 400 with violations
    - Handle PasswordResetTokenExpiredException and return 400
    - Handle PasswordResetTokenNotFoundException and return 400
    - Return 204 No Content on success
    - _Requirements: 6.4, 6.5, 6.6, 6.7_

- [ ] 9. Update AuthController
  - [ ] 9.1 Update login endpoint
    - Update LoginRequest to include password field
    - Pass password to authService.login()
    - Handle password-related exceptions
    - _Requirements: 3.1, 3.2, 3.3_
  
  - [ ] 9.2 Update activate endpoint
    - Update ActivateAccountRequest to include password field
    - Pass password to authService.activateAccount()
    - Handle PasswordPolicyViolationException
    - _Requirements: 4.1, 4.2, 4.5_

- [ ] 10. Update SecurityConfig
  - [ ] 10.1 Add password endpoints to security configuration
    - Permit /api/password/reset/request and /api/password/reset/confirm
    - Require authentication for /api/password/change
    - _Requirements: 5.1, 6.1, 6.4_

- [ ] 11. Create database migrations
  - [ ] 11.1 Create V2026.02.20.10.00.00__add_password_authentication.sql
    - Add password_hash VARCHAR(60) NULL column to users table
    - Create password_reset_tokens table with all columns
    - Add foreign key constraint to users table
    - Create indexes on token, expires_at, and user_id columns
    - Add comments for documentation
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5, 10.6_
  
  - [ ] 11.2 Create V2026.02.20.11.00.00__make_password_hash_required.sql
    - Alter password_hash column to NOT NULL
    - Update comment
    - Note: This migration should be run after all users have set passwords
    - _Requirements: 10.1_

- [ ] 12. Update infrastructure layer
  - [ ] 12.1 Extend UserEntity JPA entity
    - Add passwordHash: String? field with @Column annotation
    - _Requirements: 10.1, 10.2_
  
  - [ ] 12.2 Update UserRepositoryImpl
    - Map passwordHash field between domain and JPA entities
    - _Requirements: 10.1_

- [ ] 13. Checkpoint - Ensure integration tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 14. Add rate limiting integration
  - [ ]* 14.1 Write property test for authentication rate limiting
    - **Property 13: Authentication Rate Limiting**
    - **Validates: Requirements 7.1, 7.2**
  
  - [ ]* 14.2 Write property test for password reset rate limiting
    - **Property 14: Password Reset Rate Limiting**
    - **Validates: Requirements 7.3, 7.4**

- [ ] 15. Add audit logging integration
  - [ ]* 15.1 Write property test for audit logging
    - **Property 15: Audit Logging for Password Events**
    - **Validates: Requirements 8.1, 8.2, 8.3, 8.4, 8.5**
  
  - [ ]* 15.2 Write property test for password reset audit logging
    - **Property 11: Password Reset Audit Logging**
    - **Validates: Requirements 6.7, 8.2, 8.3**

- [ ] 16. Add performance monitoring
  - [ ]* 16.1 Write property test for password operation performance
    - **Property 16: Password Operation Performance**
    - **Validates: Requirements 9.1, 9.2, 9.3, 9.4**

- [ ] 17. Write integration tests
  - [ ]* 17.1 Write integration test for login with password
    - Test successful login with valid credentials
    - Test login failure with invalid password
    - Test login failure with missing password
    - _Requirements: 3.1, 3.2, 3.3_
  
  - [ ]* 17.2 Write integration test for account activation with password
    - Test successful activation with valid password
    - Test activation failure with invalid password
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  
  - [ ]* 17.3 Write integration test for password change
    - Test successful password change
    - Test password change with wrong current password
    - Test password change with invalid new password
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_
  
  - [ ]* 17.4 Write integration test for password reset flow
    - Test successful password reset request
    - Test successful password reset confirmation
    - Test reset with expired token
    - Test reset with invalid token
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7_
  
  - [ ]* 17.5 Write integration test for rate limiting
    - Test authentication rate limiting
    - Test password reset rate limiting
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

- [ ] 18. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Integration tests validate end-to-end flows with database and security
- The implementation follows hexagonal architecture: Domain → Application → Infrastructure → Web
- Password migration strategy: nullable field → users set passwords via reset flow → make field required
