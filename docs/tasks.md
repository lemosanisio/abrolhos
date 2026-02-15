# Implementation Plan: Security Hardening

## Overview

This implementation plan addresses four critical security improvements for the Abrolhos Kotlin/Spring Boot application: environment-specific CORS configuration, rate limiting on authentication endpoints, TOTP secret encryption at rest, and comprehensive audit logging. The implementation follows the existing layered/hexagonal architecture and integrates security features as cross-cutting concerns using Spring interceptors, JPA converters, and AOP aspects.

## Tasks

- [ ] 1. Set up project dependencies and configuration infrastructure
  - Add required dependencies to build.gradle.kts (Bucket4j, Redis, Spring AOP)
  - Create base configuration classes for security settings
  - Set up environment variable templates for security configuration
  - _Requirements: 6.1, 6.2, 6.3_

- [ ] 2. Implement CORS configuration hardening
  - [ ] 2.1 Create CorsConfig component with environment-based origin validation
    - Implement CorsConfig.kt with comma-separated origin parsing
    - Add startup validation to reject wildcards in production
    - Validate all origin URLs are well-formed
    - Configure allowed methods and headers
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_
  
  - [ ] 2.2 Write property test for CORS origin validation
    - **Property 1: CORS origin parsing**
    - **Validates: Requirements 1.1, 1.2, 1.4**
    - For any comma-separated list of valid URLs, parsing should produce a list containing exactly those URLs trimmed of whitespace
  
  - [ ] 2.3 Write unit tests for CORS edge cases
    - Test wildcard rejection in production profile
    - Test malformed URL detection
    - Test empty origin list handling
    - _Requirements: 1.1, 1.5_

- [ ] 3. Implement encryption service for TOTP secrets
  - [ ] 3.1 Create EncryptionService with AES-256-GCM
    - Implement encrypt() method with unique IV generation
    - Implement decrypt() method with key rotation support
    - Add key validation in @PostConstruct
    - Add performance monitoring (< 10ms threshold)
    - Create EncryptionException for error handling
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7_
  
  - [ ] 3.2 Write property test for encryption round-trip
    - **Property 2: Encryption round-trip consistency**
    - **Validates: Requirements 3.8**
    - For any valid TOTP secret string, encrypting then decrypting should produce an equivalent value
  
  - [ ] 3.3 Write property test for IV uniqueness
    - **Property 3: IV uniqueness**
    - **Validates: Requirements 3.3**
    - For any plaintext encrypted multiple times, each encryption should produce a different ciphertext (due to unique IVs)
  
  - [ ] 3.4 Write unit tests for encryption edge cases
    - Test empty string encryption
    - Test very long string encryption
    - Test invalid Base64 decryption
    - Test decryption with wrong key
    - Test key rotation with old keys
    - _Requirements: 3.6, 3.8_

- [ ] 4. Create JPA converter for transparent TOTP secret encryption
  - [ ] 4.1 Implement TotpSecretConverter
    - Create @Converter class implementing AttributeConverter
    - Implement convertToDatabaseColumn using EncryptionService
    - Implement convertToEntityAttribute using EncryptionService
    - Register converter with User entity
    - _Requirements: 3.1, 3.2_
  
  - [x] 4.2 Write integration test for JPA encryption
    - Test saving and loading User with TOTP secret
    - Verify database contains encrypted data
    - Verify application receives decrypted data
    - _Requirements: 3.1, 3.2_

- [ ] 5. Create database migration for encrypted secrets
  - [x] 5.1 Create Flyway migration script
    - Write V4__encrypt_totp_secrets.sql to alter column size
    - Add column comment documenting encryption format
    - _Requirements: 4.1, 4.5_
  
  - [-] 5.2 Create data migration script for existing secrets
    - Write Kotlin script to encrypt all existing plaintext secrets
    - Add rollback capability for failed migrations
    - Add verification step to confirm all secrets encrypted
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.6_
  
  - [ ] 5.3 Write test for migration script
    - Test migration with sample plaintext secrets
    - Verify authentication still works after migration
    - Test rollback on encryption failure
    - _Requirements: 4.3, 4.6_

- [ ] 6. Implement rate limiting infrastructure
  - [x] 6.1 Create RateLimitService with Redis backend
    - Implement tryConsume() method using Redis ZSET
    - Add sliding window logic with old entry cleanup
    - Implement exponential backoff calculation
    - Add graceful degradation for Redis failures
    - _Requirements: 2.1, 2.2, 2.4_
  
  - [x] 6.2 Write property test for rate limit enforcement
    - **Property 4: Rate limit enforcement**
    - **Validates: Requirements 2.2, 2.3**
    - For any client making N+1 requests within the time window (where N is the limit), the (N+1)th request should be rejected
  
  - [x] 6.3 Write property test for rate limit window reset
    - **Property 5: Rate limit window reset**
    - **Validates: Requirements 2.4**
    - For any client that exceeds the rate limit, after the time window expires, the client should be able to make requests again
  
  - [x] 6.4 Write unit tests for rate limiting edge cases
    - Test Redis connection failure (fail open)
    - Test exponential backoff calculation
    - Test window boundary conditions
    - _Requirements: 2.4_

- [ ] 7. Create rate limit filter for authentication endpoints
  - [x] 7.1 Implement RateLimitFilter as HandlerInterceptor
    - Create filter implementing HandlerInterceptor
    - Add client IP extraction from X-Forwarded-For header
    - Implement preHandle() with rate limit checking
    - Add rate limit headers to responses
    - Return 429 status with Retry-After header when limited
    - Register filter for /api/auth/* endpoints
    - _Requirements: 2.1, 2.2, 2.3, 2.5, 2.6, 2.7, 2.8, 2.9_
  
  - [x] 7.2 Write integration test for rate limit filter
    - Test successful requests under limit
    - Test 429 response when limit exceeded
    - Test rate limit headers in response
    - Test Retry-After header calculation
    - _Requirements: 2.3, 2.9_

- [x] 8. Implement audit logging infrastructure
  - [x] 8.1 Create AuditLogger component
    - Create AuditEvent data class with all required fields
    - Implement logLoginAttempt(), logLoginSuccess(), logLoginFailure()
    - Implement logAccountActivation(), logRateLimitExceeded()
    - Implement logCorsRejected(), logTokenValidation()
    - Add JSON serialization for structured logging
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8_
  
  - [x] 8.2 Write unit tests for audit logger
    - Test all audit event types are logged correctly
    - Test JSON format is valid and parseable
    - Test sensitive data is not logged
    - _Requirements: 5.8_

- [x] 9. Create AOP aspect for authentication audit logging
  - [x] 9.1 Implement AuditAspect with @Around advice
    - Create @Aspect component with AuditLogger dependency
    - Add @Around advice for AuthService.login()
    - Add @Around advice for AuthService.activateAccount()
    - Extract client IP and user agent from request context
    - Log attempt, success, and failure events
    - _Requirements: 5.1, 5.2, 5.3, 5.4_
  
  - [x] 9.2 Write integration test for audit aspect
    - Test login attempts are logged
    - Test login success is logged
    - Test login failure is logged with reason
    - Test account activation is logged
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 10. Configure Logback for audit logging
  - [x] 10.1 Create logback-spring.xml configuration
    - Add AUDIT_FILE appender with rolling policy
    - Configure 90-day retention for audit logs
    - Add ASYNC_AUDIT appender for non-blocking writes
    - Configure AUDIT logger to use async appender
    - Set up separate audit.log file
    - _Requirements: 5.6, 5.7_
  
  - [x] 10.2 Write test for audit log file creation
    - Test audit events are written to separate file
    - Test log rotation configuration
    - Test async appender configuration
    - _Requirements: 5.6, 5.7_

- [x] 11. Integrate audit logging with rate limit filter
  - [x] 11.1 Add audit logging calls to RateLimitFilter
    - Call auditLogger.logRateLimitExceeded() when limit exceeded
    - Include client IP and endpoint in audit event
    - _Requirements: 5.4_
  
  - [x] 11.2 Write integration test for rate limit audit logging
    - Test rate limit exceeded events are logged
    - Verify audit log contains correct client IP and endpoint
    - _Requirements: 5.4_

- [x] 12. Create encryption key generation utility
  - [x] 12.1 Write GenerateEncryptionKey.kt script
    - Generate 256-bit AES key using KeyGenerator
    - Encode key as Base64
    - Print formatted environment variable
    - _Requirements: 3.5_

- [x] 13. Add configuration validation and documentation
  - [x] 13.1 Document all environment variables
    - Create configuration documentation with all required variables
    - Add example values for each environment
    - Document validation rules and constraints
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6_
  
  - [x] 13.2 Add startup validation for required configuration
    - Validate SECURITY_CORS_ALLOWED_ORIGINS is set
    - Validate SECURITY_ENCRYPTION_KEY is set and valid
    - Validate rate limit configuration values
    - Fail fast with clear error messages for missing config
    - _Requirements: 6.4, 6.5_

- [ ] 14. Checkpoint - Ensure all tests pass
  - Run all unit tests and property-based tests
  - Verify encryption round-trip works correctly
  - Verify rate limiting enforces limits
  - Verify audit events are logged
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 15. Integration and end-to-end testing
  - [ ] 15.1 Write integration tests for complete authentication flow
    - Test login with CORS validation
    - Test login with rate limiting
    - Test login with encrypted TOTP secrets
    - Test login with audit logging
    - Verify all security features work together
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_
  
  - [ ] 15.2 Write property test for backward compatibility
    - **Property 6: API compatibility**
    - **Validates: Requirements 7.1, 7.2, 7.3**
    - For any valid authentication request, the response format should remain unchanged after security hardening
  
  - [ ] 15.3 Write integration tests for error scenarios
    - Test Redis unavailable (rate limiting fails open)
    - Test encryption key rotation
    - Test CORS rejection
    - Test rate limit exceeded
    - _Requirements: 1.3, 2.3, 3.8_

- [ ] 16. Final checkpoint - Complete security hardening validation
  - Verify all security features are enabled
  - Test with production-like configuration
  - Verify performance requirements met (< 10ms overhead)
  - Review audit logs for completeness
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties with minimum 100 iterations
- Unit tests validate specific examples and edge cases
- Integration tests verify components work together correctly
- Checkpoints ensure incremental validation at key milestones
- All security features integrate transparently with existing authentication flow
- Performance requirement: < 10ms overhead per operation
- Redis failures should fail open with logging to maintain availability
