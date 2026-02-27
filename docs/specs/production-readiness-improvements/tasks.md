# Implementation Plan: Production Readiness Improvements

## Overview

This implementation plan breaks down the production readiness improvements into three phases: Critical Security, Observability, and Code Quality. Each phase contains discrete coding tasks that build incrementally, with property-based tests to validate correctness properties from the design document.

The implementation assumes a single-instance, self-hosted deployment using VictoriaMetrics, VictoriaLogs, and Zipkin for observability.

## Tasks

### Phase 1: Critical Security

- [ ] 1. Implement JWT secret validation at startup
  - Create `JwtSecretValidator` component with `@PostConstruct` method
  - Validate JWT secret is at least 32 characters long
  - Throw `IllegalStateException` with clear error message if validation fails
  - Add INFO level logging for successful validation
  - _Requirements: 1.1, 1.2, 1.3_

- [ ]* 1.1 Write example tests for JWT secret validation
  - Test application startup with 31-character secret (should fail)
  - Test application startup with 32-character secret (should succeed)
  - Test application startup with 64-character secret (should succeed)
  - Test empty secret (should fail)
  - _Requirements: 1.1, 1.2, 1.3_

- [ ] 2. Update actuator endpoint security configuration
  - Modify `SecurityConfig` to require ADMIN role for actuator endpoints
  - Keep `/actuator/health` and `/actuator/health/**` publicly accessible
  - Ensure unauthenticated requests return 401
  - Ensure non-ADMIN authenticated requests return 403
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [ ]* 2.1 Write property test for actuator endpoint authorization
  - **Property 3: Actuator Endpoint Authorization**
  - **Validates: Requirements 2.1, 2.2, 2.3**
  - _Requirements: 2.1, 2.2, 2.3_

- [ ]* 2.2 Write example test for health endpoint public access
  - Test `/actuator/health` is accessible without authentication
  - _Requirements: 2.4_

- [ ] 3. Add generic exception handler to GlobalExceptionHandler
  - Add `@ExceptionHandler(Exception::class)` method
  - Retrieve correlation ID from MDC
  - Log exception with correlation ID, type, message, and stack trace at ERROR level
  - Return HTTP 500 with `ErrorResponse` including correlation ID
  - Ensure no internal exception details are exposed to clients
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ] 4. Update ErrorResponse data class to include correlation ID
  - Add optional `correlationId: String?` field to `ErrorResponse`
  - Add `timestamp: Instant` field with default value
  - _Requirements: 3.4_

- [ ]* 4.1 Write property test for generic exception handling
  - **Property 2: Generic Exception Handling with Correlation ID**
  - **Validates: Requirements 3.1, 3.2, 3.3, 3.4**
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [ ] 5. Checkpoint - Ensure all Phase 1 tests pass
  - Ensure all tests pass, ask the user if questions arise.

### Phase 2: Observability

- [ ] 6. Implement correlation ID filter
  - Create `CorrelationIdFilter` extending `OncePerRequestFilter`
  - Set filter order to `Ordered.HIGHEST_PRECEDENCE`
  - Check for `X-Correlation-ID` header in incoming request
  - Generate new UUID if header is absent
  - Store correlation ID in MDC with key `correlationId`
  - Add correlation ID to response headers as `X-Correlation-ID`
  - Clear MDC in finally block after request completes
  - _Requirements: 4.1, 4.2, 4.3, 4.5, 4.6_

- [ ]* 6.1 Write property test for correlation ID lifecycle
  - **Property 1: Correlation ID Lifecycle**
  - **Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.6**
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

- [ ] 7. Update logback configuration to include correlation ID
  - Modify `logback-spring.xml` to add `%X{correlationId}` to log pattern
  - Ensure JSON/ECS format includes correlation ID as separate field
  - Test that logs include correlation ID when available
  - _Requirements: 4.4_

- [ ] 8. Add Micrometer Tracing dependencies
  - Add `io.micrometer:micrometer-tracing-bridge-brave` to build.gradle.kts
  - Add `io.zipkin.reporter2:zipkin-reporter-brave` to build.gradle.kts
  - _Requirements: 5.1_

- [ ] 9. Configure Micrometer Tracing for Zipkin
  - Add tracing configuration to `application.yml`
  - Set sampling probability to 1.0 (100% for single instance)
  - Configure Zipkin endpoint URL (default: http://localhost:9411/api/v2/spans)
  - Create `TracingConfig` component with `zipkinSpanHandler` bean
  - Ensure async sending with queue for resilience
  - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ]* 9.1 Write example test for Zipkin configuration
  - Test that tracing configuration is loaded correctly at startup
  - _Requirements: 5.1_

- [ ]* 9.2 Write property test for tracing span creation
  - **Property 4: Tracing Span Creation**
  - **Validates: Requirements 5.2, 5.3, 5.4**
  - _Requirements: 5.2, 5.3, 5.4_

- [ ] 10. Add cache metrics to MetricsService
  - Add `cache.hits` counter with `cache=redis` tag
  - Add `cache.misses` counter with `cache=redis` tag
  - Add public methods `recordCacheHit()` and `recordCacheMiss()`
  - _Requirements: 6.3_

- [ ] 11. Integrate cache metrics into caching layer
  - Identify cache hit/miss points in code
  - Add metrics recording calls to cache operations
  - Ensure metrics are recorded for all cache interactions
  - _Requirements: 6.3_

- [ ]* 11.1 Write property test for cache metrics
  - **Property 7: Cache Metrics**
  - **Validates: Requirements 6.3**
  - _Requirements: 6.3_

- [ ] 12. Enhance authentication metrics with tags
  - Update `MetricsService` to add tags to authentication metrics
  - Add `success` tag (true/false) to authentication attempt counter
  - Add optional `reason` tag for failure reasons
  - Update `AuthService` to record metrics with appropriate tags
  - _Requirements: 6.2_

- [ ]* 12.1 Write property test for authentication metrics
  - **Property 6: Authentication Metrics**
  - **Validates: Requirements 6.2**
  - _Requirements: 6.2_

- [ ]* 12.2 Write property test for post creation metrics
  - **Property 5: Post Creation Metrics**
  - **Validates: Requirements 6.1**
  - _Requirements: 6.1_

- [ ]* 12.3 Write example test for metrics endpoint format
  - Test `/actuator/prometheus` returns Prometheus-formatted metrics
  - _Requirements: 6.4_

- [ ] 13. Checkpoint - Ensure all Phase 2 tests pass
  - Ensure all tests pass, ask the user if questions arise.

### Phase 3: Code Quality

- [ ] 14. Resolve all TODO comments in codebase
  - Search codebase for TODO comments: `grep -r "TODO" src/`
  - For each TODO: complete functionality, document in issue tracker, or remove
  - Verify no TODO comments remain in production code
  - _Requirements: 7.1, 7.2, 7.3_

- [ ] 15. Audit and remove debug secret logging
  - Search for JWT token logging: `grep -r "jwt\|token" src/ | grep -i "log"`
  - Search for password logging: `grep -r "password" src/ | grep -i "log"`
  - Search for API key logging: `grep -r "api.*key\|secret" src/ | grep -i "log"`
  - Remove or redact any sensitive data logging
  - _Requirements: 8.1, 8.2_

- [ ] 16. Implement sensitive data redaction utility
  - Create `SensitiveDataRedactor` object with redaction functions
  - Implement `redactJwt(jwt: String): String` - show first 10 chars only
  - Implement `redactPassword(password: String): String` - return "***"
  - Implement `redactEmail(email: String): String` - mask middle characters
  - _Requirements: 8.3_

- [ ]* 16.1 Write property test for sensitive data redaction
  - **Property 12: Sensitive Data Redaction**
  - **Validates: Requirements 8.3**
  - _Requirements: 8.3_

- [ ]* 16.2 Write unit tests for specific redaction examples
  - Test JWT redaction with various lengths
  - Test password redaction
  - Test email redaction with various formats
  - _Requirements: 8.3_

- [ ] 17. Implement security headers filter
  - Create `SecurityHeadersFilter` extending `OncePerRequestFilter`
  - Add `X-Content-Type-Options: nosniff` header
  - Add `X-Frame-Options: DENY` header
  - Add `X-XSS-Protection: 1; mode=block` header
  - Add `Strict-Transport-Security` header (production only)
  - Add `Content-Security-Policy: default-src 'self'` header
  - Make headers configurable via application properties
  - _Requirements: 9.1_

- [ ]* 17.1 Write property test for security headers
  - **Property 8: Security Headers in Responses**
  - **Validates: Requirements 9.1**
  - _Requirements: 9.1_

- [ ]* 17.2 Write example tests for specific security headers
  - Test each security header is present with correct value
  - Test HSTS header only in production profile
  - _Requirements: 9.1_

- [ ] 18. Configure HTTPS redirect for production
  - Update `SecurityConfig` with `@Profile("prod")` configuration
  - Add `requiresChannel { anyRequest { requiresSecure() } }` configuration
  - Ensure HTTP requests redirect to HTTPS with 301 status
  - _Requirements: 9.2_

- [ ]* 18.1 Write property test for HTTPS redirect
  - **Property 9: HTTPS Redirect**
  - **Validates: Requirements 9.2**
  - _Requirements: 9.2_

- [ ] 19. Review and validate CORS configuration
  - Review existing `CorsConfig` implementation
  - Verify `allowedOrigins` is explicitly configured (no wildcards)
  - Ensure origins are validated against whitelist from environment variable
  - Confirm credentials are only allowed for trusted origins
  - Document CORS configuration in README
  - _Requirements: 9.3_

- [ ]* 19.1 Write property test for CORS origin validation
  - **Property 10: CORS Origin Validation**
  - **Validates: Requirements 9.3**
  - _Requirements: 9.3_

- [ ]* 19.2 Write example test for CORS preflight request
  - Test CORS preflight request handling
  - Test allowed origin is accepted
  - Test disallowed origin is rejected
  - _Requirements: 9.3_

- [ ] 20. Review and validate input validation
  - Review controllers for `@Valid` annotations on parameters
  - Review service classes for `@Validated` annotations
  - Verify custom validators for business rules
  - Confirm SQL injection prevention (JPA/Hibernate)
  - Confirm XSS prevention (JSON serialization)
  - _Requirements: 9.5_

- [ ]* 20.1 Write property test for input validation
  - **Property 11: Input Validation and Sanitization**
  - **Validates: Requirements 9.5**
  - _Requirements: 9.5_

- [ ] 21. Fix IP spoofing vulnerability in rate limiting and auditing
  - Add `server.forward-headers-strategy: framework` to application.yml
  - Update `RateLimitFilter` to use `request.remoteAddr` instead of `X-Forwarded-For`
  - Update `AuditAspect` to use `request.remoteAddr` instead of `X-Forwarded-For`
  - Remove manual `X-Forwarded-For` header parsing
  - Test that rate limiting cannot be bypassed with spoofed headers
  - _Requirements: 9.6_

- [ ]* 21.1 Write property test for client IP extraction security
  - **Property 13: Client IP Extraction Security**
  - **Validates: Requirements 9.6**
  - _Requirements: 9.6_

- [ ]* 21.2 Write example test for rate limiting with spoofed header
  - Test that spoofed `X-Forwarded-For` header does not bypass rate limiting
  - Test that actual client IP is used for rate limiting
  - _Requirements: 9.6_

- [ ] 22. Review rate limiting configuration
  - Verify `rate-limit.auth.max-attempts` is appropriate (default: 5)
  - Verify `rate-limit.auth.window-seconds` is appropriate (default: 300)
  - Confirm rate limiting is applied to authentication endpoints
  - Confirm Redis-backed rate limiting is working
  - _Requirements: 9.4_

- [ ] 23. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 24. Update documentation
  - Update README with new environment variables (ZIPKIN_ENDPOINT)
  - Document correlation ID header (`X-Correlation-ID`) for API consumers
  - Document actuator security changes for ops team
  - Update deployment guide with Zipkin configuration
  - Add troubleshooting guide for observability features
  - Document forward headers strategy requirement for reverse proxy

## Notes

- Tasks marked with `*` are optional property-based and unit tests
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation at phase boundaries
- Property tests validate universal correctness properties from design document
- Unit tests validate specific examples and edge cases
- All tests should use Kotest with minimum 100 iterations for property tests
- Implementation should be done in order to maintain dependencies between phases
