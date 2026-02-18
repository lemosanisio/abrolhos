# Security Hardening Improvements for Abrolhos

## Overview
This document outlines critical security improvements needed for the Abrolhos application to address vulnerabilities in CORS configuration, authentication rate limiting, secret storage, and audit logging.

## Current Security Issues

### 1. CORS Wildcard Configuration
**Location**: `Backend/abrolhos/src/main/kotlin/br/dev/demoraes/abrolhos/application/config/SecurityConfig.kt`

**Issue**: CORS is configured with wildcard `allowedOrigins = listOf("*")`, allowing any origin to make requests to the API.

**Risk Level**: HIGH
- Enables CSRF attacks from malicious websites
- Allows unauthorized domains to access API
- Exposes sensitive endpoints to any origin

### 2. Missing Rate Limiting Enforcement
**Location**: Authentication endpoints in `AuthController.kt`

**Issue**: Rate limiting configuration exists in `application.yml` but is not enforced on authentication endpoints.

**Risk Level**: CRITICAL
- Enables brute force attacks on TOTP codes
- Allows credential stuffing attacks
- No protection against DoS on auth endpoints

### 3. Unencrypted TOTP Secrets
**Location**: `Backend/abrolhos/src/main/kotlin/br/dev/demoraes/abrolhos/domain/entities/User.kt`

**Issue**: TOTP secrets stored as plain text in PostgreSQL database.

**Risk Level**: CRITICAL
- Database breach exposes all user authentication secrets
- Compromised backups reveal TOTP secrets
- No defense-in-depth for authentication credentials

### 4. No Audit Logging
**Location**: All authentication and authorization operations

**Issue**: No audit trail for sensitive operations (login attempts, account activation, token generation).

**Risk Level**: HIGH
- Cannot detect security incidents
- No forensic capability after breach
- Compliance issues (GDPR, SOC2)

---

## Proposed Solutions

### Solution 1: Restrict CORS to Specific Origins

#### Requirements
```xml
<requirement id="SEC-001" priority="high">
  <title>Configure CORS with Specific Origins</title>
  <description>
    Replace wildcard CORS configuration with environment-specific allowed origins
  </description>
  <acceptance-criteria>
    - CORS configuration reads allowed origins from environment variables
    - Default configuration includes only localhost for development
    - Production configuration requires explicit origin list
    - Invalid origins receive 403 Forbidden response
    - CORS preflight requests handled correctly
  </acceptance-criteria>
</requirement>
```

#### Implementation Details
- Add `ALLOWED_ORIGINS` environment variable (comma-separated list)
- Update `SecurityConfig.kt` to parse and validate origins
- Add validation to reject wildcard in production
- Document origin configuration in README

#### Testing Strategy
- Unit tests for origin parsing and validation
- Integration tests for CORS preflight requests
- Property-based tests for origin matching logic
- Manual testing with different origin configurations

---

### Solution 2: Implement Rate Limiting on Auth Endpoints

#### Requirements
```xml
<requirement id="SEC-002" priority="critical">
  <title>Enforce Rate Limiting on Authentication Endpoints</title>
  <description>
    Implement and enforce rate limiting on all authentication endpoints to prevent brute force attacks
  </description>
  <acceptance-criteria>
    - Rate limiting applied to /api/auth/login endpoint
    - Rate limiting applied to /api/auth/activate endpoint
    - Configurable limits per IP address (default: 5 attempts per 15 minutes)
    - 429 Too Many Requests response when limit exceeded
    - Rate limit headers included in responses (X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset)
    - Rate limit state persisted (Redis or in-memory with clustering support)
    - Exponential backoff for repeated violations
  </acceptance-criteria>
</requirement>
```

#### Implementation Details
- Use Bucket4j library for rate limiting
- Implement `RateLimitingFilter` for auth endpoints
- Add Redis integration for distributed rate limiting
- Configure limits via environment variables:
  - `RATE_LIMIT_AUTH_CAPACITY` (default: 5)
  - `RATE_LIMIT_AUTH_REFILL_MINUTES` (default: 15)
- Add custom exception `RateLimitExceededException`
- Update `GlobalExceptionHandler` to handle rate limit exceptions

#### Testing Strategy
- Unit tests for rate limiting logic
- Integration tests simulating brute force attempts
- Property-based tests for rate limit calculations
- Load tests to verify performance impact
- Tests for distributed rate limiting with Redis

---

### Solution 3: Encrypt TOTP Secrets at Rest

#### Requirements
```xml
<requirement id="SEC-003" priority="critical">
  <title>Encrypt TOTP Secrets in Database</title>
  <description>
    Implement encryption for TOTP secrets before storing in database, with secure key management
  </description>
  <acceptance-criteria>
    - TOTP secrets encrypted using AES-256-GCM before database storage
    - Encryption key stored securely (environment variable or key management service)
    - Encryption key rotation supported without breaking existing secrets
    - Decryption transparent to application logic
    - Migration script to encrypt existing plain text secrets
    - Encryption/decryption performance impact &lt; 10ms per operation
    - Failed decryption handled gracefully with audit log entry
  </acceptance-criteria>
</requirement>
```

#### Implementation Details
- Create `EncryptionService` with AES-256-GCM implementation
- Add `@Converter` for automatic encryption/decryption in JPA
- Store encryption key in environment variable `TOTP_ENCRYPTION_KEY`
- Implement key versioning for rotation support
- Add Flyway migration to encrypt existing secrets
- Update `TotpSecret` value object to handle encrypted format

#### Key Management Options
1. **Environment Variable** (simple, suitable for single instance)
2. **AWS KMS** (recommended for production)
3. **HashiCorp Vault** (enterprise option)

#### Testing Strategy
- Unit tests for encryption/decryption round-trip
- Property-based tests for encryption correctness
- Integration tests with database persistence
- Performance tests for encryption overhead
- Migration tests for existing data
- Tests for key rotation scenarios

---

### Solution 4: Implement Comprehensive Audit Logging

#### Requirements
```xml
<requirement id="SEC-004" priority="high">
  <title>Add Audit Logging for Security Events</title>
  <description>
    Implement comprehensive audit logging for all authentication, authorization, and sensitive operations
  </description>
  <acceptance-criteria>
    - All login attempts logged (success and failure)
    - Account activation events logged
    - Token generation and validation logged
    - Failed authentication attempts include reason
    - Audit logs include: timestamp, username, IP address, user agent, action, result
    - Audit logs stored separately from application logs
    - Audit logs immutable (append-only)
    - Audit logs retained for minimum 90 days
    - Structured logging format (JSON) for easy parsing
    - Sensitive data (TOTP codes, tokens) never logged
  </acceptance-criteria>
</requirement>
```

#### Events to Audit
- `USER_LOGIN_SUCCESS`
- `USER_LOGIN_FAILURE`
- `USER_ACTIVATION_SUCCESS`
- `USER_ACTIVATION_FAILURE`
- `INVITE_TOKEN_GENERATED`
- `INVITE_TOKEN_VALIDATED`
- `JWT_TOKEN_GENERATED`
- `JWT_TOKEN_VALIDATION_FAILED`
- `RATE_LIMIT_EXCEEDED`
- `TOTP_SECRET_GENERATED`
- `TOTP_SECRET_ENCRYPTION_FAILED`

#### Implementation Details
- Create `AuditService` for centralized audit logging
- Define `AuditEvent` data class with required fields
- Use separate logger for audit events (`audit.log`)
- Implement `AuditEventRepository` for database storage
- Add `@Audit` annotation for automatic audit logging
- Configure Logback for JSON structured logging
- Add audit log rotation and retention policies

#### Audit Log Schema
```kotlin
data class AuditEvent(
    val id: UUID,
    val timestamp: Instant,
    val eventType: AuditEventType,
    val username: Username?,
    val ipAddress: String,
    val userAgent: String?,
    val action: String,
    val result: AuditResult,
    val details: Map<String, String>,
    val errorMessage: String?
)
```

#### Testing Strategy
- Unit tests for audit event creation
- Integration tests verifying audit logs written
- Tests ensuring sensitive data not logged
- Property-based tests for audit log format
- Tests for audit log retention policies

---

## Implementation Priority

1. **Phase 1 (Critical - Week 1)**
   - SEC-002: Rate Limiting (prevents immediate attacks)
   - SEC-003: TOTP Secret Encryption (protects existing data)

2. **Phase 2 (High - Week 2)**
   - SEC-001: CORS Configuration (hardens API access)
   - SEC-004: Audit Logging (enables detection)

3. **Phase 3 (Ongoing)**
   - Security testing and validation
   - Documentation updates
   - Team training on new security features

---

## Security Testing Checklist

### CORS Testing
- [ ] Verify allowed origins accept requests
- [ ] Verify disallowed origins rejected
- [ ] Test preflight OPTIONS requests
- [ ] Test with credentials
- [ ] Test wildcard rejection in production

### Rate Limiting Testing
- [ ] Verify rate limits enforced per IP
- [ ] Test limit reset after time window
- [ ] Test 429 response format
- [ ] Test rate limit headers
- [ ] Load test with concurrent requests
- [ ] Test distributed rate limiting with multiple instances

### Encryption Testing
- [ ] Verify encryption/decryption round-trip
- [ ] Test with different key sizes
- [ ] Test key rotation
- [ ] Test migration of existing secrets
- [ ] Performance test encryption overhead
- [ ] Test failed decryption handling

### Audit Logging Testing
- [ ] Verify all security events logged
- [ ] Verify sensitive data not logged
- [ ] Test log format and structure
- [ ] Test log retention policies
- [ ] Test log immutability
- [ ] Query and analyze audit logs

---

## Compliance Considerations

### GDPR
- Audit logs may contain personal data (username, IP)
- Implement data retention policies
- Support data deletion requests
- Document data processing activities

### SOC 2
- Audit logging required for Type II compliance
- Rate limiting demonstrates security controls
- Encryption at rest required for sensitive data
- Document security policies and procedures

### OWASP Top 10
- **A01:2021 - Broken Access Control**: CORS fixes
- **A02:2021 - Cryptographic Failures**: TOTP encryption
- **A07:2021 - Identification and Authentication Failures**: Rate limiting
- **A09:2021 - Security Logging and Monitoring Failures**: Audit logging

---

## Monitoring and Alerting

### Metrics to Track
- Rate limit violations per hour
- Failed login attempts per user
- Encryption/decryption errors
- Audit log write failures
- CORS rejection rate

### Alerts to Configure
- **Critical**: Multiple rate limit violations from same IP
- **Critical**: Encryption/decryption failures
- **High**: Unusual number of failed login attempts
- **High**: Audit log write failures
- **Medium**: High CORS rejection rate

---

## Documentation Updates Required

1. **README.md**
   - Add security configuration section
   - Document environment variables
   - Add security best practices

2. **API Documentation**
   - Document rate limit headers
   - Document 429 response format
   - Update CORS documentation

3. **Deployment Guide**
   - Add encryption key setup
   - Add Redis setup for rate limiting
   - Add audit log configuration

4. **Security Policy**
   - Create SECURITY.md
   - Document vulnerability reporting
   - Document security update process

---

## Dependencies and Tools

### New Dependencies
```kotlin
// Rate Limiting
implementation("com.bucket4j:bucket4j-core:8.7.0")
implementation("org.springframework.boot:spring-boot-starter-data-redis")

// Encryption
implementation("org.bouncycastle:bcprov-jdk18on:1.77")

// Structured Logging
implementation("net.logstash.logback:logstash-logback-encoder:7.4")
```

### Infrastructure Requirements
- Redis instance for distributed rate limiting
- Increased database storage for audit logs
- Log aggregation system (ELK, Splunk, or CloudWatch)
- Key management service (optional, for production)

---

## Rollback Plan

### If Issues Arise
1. **CORS**: Revert to wildcard temporarily, document affected origins
2. **Rate Limiting**: Disable filter, increase limits significantly
3. **Encryption**: Keep migration script, decrypt if needed
4. **Audit Logging**: Disable audit event persistence, keep in-memory

### Rollback Triggers
- Performance degradation > 20%
- Error rate increase > 5%
- User complaints about access issues
- Production incidents related to changes

---

## Success Metrics

### Security Metrics
- Zero successful brute force attacks
- 100% of TOTP secrets encrypted
- 100% of security events audited
- Zero unauthorized CORS requests in production

### Performance Metrics
- Rate limiting overhead < 5ms per request
- Encryption overhead < 10ms per operation
- Audit logging overhead < 3ms per event
- No increase in API response times

### Operational Metrics
- Audit logs queryable within 1 minute
- Rate limit violations detected in real-time
- Security incidents detected within 5 minutes
- Mean time to respond to security events < 15 minutes
