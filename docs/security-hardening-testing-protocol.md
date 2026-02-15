# Security Hardening Testing Protocol

## Overview

This document provides comprehensive manual checkpoint procedures and end-to-end testing protocols for validating the security hardening implementation in the Abrolhos application. It covers Tasks 14, 15, and 16 from the implementation plan.

## Task 14: Manual Checkpoint - Test Suite Validation

### Objective
Verify that all automated tests pass and that core security features function correctly in isolation.

### Prerequisites
- All code from Tasks 1-13 has been implemented
- Redis is running (for rate limiting tests)
- Environment variables are configured in `local.env`

### Procedure

#### Step 1: Environment Setup Verification

```bash
# Verify Redis is running
docker ps | grep redis
# Expected: Container running on port 6379

# Verify environment variables are set
grep -E "SECURITY_" local.env
# Expected: All required SECURITY_* variables present
```

#### Step 2: Run Complete Test Suite

```bash
# Run all tests with verbose output
./gradlew test --info

# Expected output:
# - All unit tests pass (137+ tests)
# - All property-based tests pass (5 properties, 100 iterations each)
# - All integration tests pass
# - No compilation errors
# - No test failures
```

#### Step 3: Verify Test Coverage by Component

Run tests by component and verify expected results:

**CORS Configuration Tests:**
```bash
./gradlew test --tests "*CorsConfigTest"

# Verify:
# ✓ 31 tests pass
# ✓ Wildcard rejection in production
# ✓ Malformed URL detection
# ✓ Origin parsing with whitespace
```

**Encryption Service Tests:**
```bash
./gradlew test --tests "*EncryptionServiceTest"

# Verify:
# ✓ 33 tests pass
# ✓ Round-trip encryption works
# ✓ IV uniqueness verified
# ✓ Key rotation supported
# ✓ Invalid key detection
```

**Rate Limiting Tests:**
```bash
./gradlew test --tests "*RateLimitServiceTest"
./gradlew test --tests "*RateLimitFilterTest"

# Verify:
# ✓ 30+ service tests pass
# ✓ 19 filter tests pass
# ✓ Sliding window enforcement
# ✓ Exponential backoff calculation
# ✓ Redis failure handling (fail open)
```

**Audit Logging Tests:**
```bash
./gradlew test --tests "*AuditLoggerTest"
./gradlew test --tests "*AuditAspectIntegrationTest"
./gradlew test --tests "*AuditLogConfigurationTest"

# Verify:
# ✓ 10 logger tests pass
# ✓ 8 aspect integration tests pass
# ✓ 2 configuration tests pass
# ✓ All event types logged correctly
# ✓ JSON format valid
```

#### Step 4: Verify Property-Based Tests

Property-based tests should run 100 iterations each:

```bash
./gradlew test --tests "*PropertyTest"

# Verify all properties pass:
# ✓ Property 1: CORS origin parsing
# ✓ Property 2: Encryption round-trip consistency
# ✓ Property 3: IV uniqueness
# ✓ Property 4: Rate limit enforcement
# ✓ Property 5: Rate limit window reset
```

#### Step 5: Manual Verification Questions

Answer these questions based on test results:

1. **Do all 137+ tests pass without failures?**
   - [ ] Yes - Proceed to next step
   - [ ] No - Review failures, fix issues, re-run tests

2. **Do property-based tests complete 100 iterations each?**
   - [ ] Yes - Proceed to next step
   - [ ] No - Check for infinite loops or performance issues

3. **Are there any compilation warnings or errors?**
   - [ ] No warnings/errors - Proceed to next step
   - [ ] Yes - Review and address warnings

4. **Does encryption round-trip work correctly?**
   - [ ] Yes - Verified by Property 2
   - [ ] No - Check encryption key configuration

5. **Does rate limiting enforce limits correctly?**
   - [ ] Yes - Verified by Property 4
   - [ ] No - Check Redis connection and configuration

6. **Are audit events logged correctly?**
   - [ ] Yes - Verified by audit tests
   - [ ] No - Check logback configuration

### Checkpoint Completion Criteria

- [ ] All automated tests pass (137+ tests)
- [ ] All property-based tests complete 100 iterations
- [ ] No compilation errors or warnings
- [ ] All manual verification questions answered "Yes"

### Troubleshooting

**If tests fail:**
1. Check Redis is running: `docker ps | grep redis`
2. Verify environment variables: `cat local.env`
3. Check test logs: `./gradlew test --info`
4. Review specific test failures in `build/reports/tests/test/index.html`

**If property tests fail:**
1. Check for counterexamples in test output
2. Verify the property logic is correct
3. Check if the implementation has edge case bugs


---

## Task 15: End-to-End Testing Protocol

### Objective
Verify that all security features work together correctly in a complete authentication flow and handle error scenarios gracefully.

### Prerequisites
- Task 14 checkpoint completed successfully
- Application is running: `./gradlew bootRun`
- Redis is running
- Frontend or API testing tool available (curl, Postman, or HTTPie)

### 15.1: Complete Authentication Flow Integration Test

#### Test Scenario 1: Successful Login with All Security Features

**Setup:**
```bash
# Ensure application is running
./gradlew bootRun

# In another terminal, verify services
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

**Test Steps:**

1. **Verify CORS Protection:**
```bash
# Request from allowed origin (should succeed)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Origin: http://localhost:3000" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass","totpCode":"123456"}' \
  -v

# Verify response headers include:
# Access-Control-Allow-Origin: http://localhost:3000
# Access-Control-Allow-Credentials: true

# Request from disallowed origin (should fail)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Origin: http://evil.com" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"testpass","totpCode":"123456"}' \
  -v

# Verify: No CORS headers in response (request blocked)
```

**Expected Results:**
- [ ] Allowed origin receives CORS headers
- [ ] Disallowed origin does not receive CORS headers
- [ ] Application logs CORS rejection in audit log

2. **Verify Rate Limiting:**
```bash
# Make 5 login attempts (within limit)
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"testuser","password":"wrong","totpCode":"000000"}' \
    -v
done

# Check rate limit headers in responses:
# X-RateLimit-Limit: 5
# X-RateLimit-Remaining: 4, 3, 2, 1, 0

# Make 6th attempt (should be rate limited)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"wrong","totpCode":"000000"}' \
  -v

# Expected: HTTP 429 Too Many Requests
# Expected header: Retry-After: <seconds>
```

**Expected Results:**
- [ ] First 5 requests return 401 (authentication failed)
- [ ] 6th request returns 429 (rate limited)
- [ ] Rate limit headers present in all responses
- [ ] Retry-After header present in 429 response
- [ ] Audit log contains RATE_LIMIT_EXCEEDED event

3. **Verify TOTP Secret Encryption:**
```bash
# Create a test user with TOTP secret
# (Use your existing user creation endpoint or database script)

# Connect to database and verify encryption
docker exec -it abrolhos-postgres psql -U abrolhos -d abrolhos -c \
  "SELECT username, totp_secret FROM users WHERE username='testuser';"

# Verify:
# - totp_secret column contains encrypted data (not plaintext base32)
# - Value looks like: "iv:ciphertext" format
# - Value is different from original plaintext secret
```

**Expected Results:**
- [ ] TOTP secret in database is encrypted (not plaintext)
- [ ] Encrypted value contains IV and ciphertext
- [ ] User can still authenticate with TOTP code (decryption works)

4. **Verify Audit Logging:**
```bash
# Check audit log file exists
ls -la logs/audit.log

# View recent audit events
tail -n 20 logs/audit.log | jq .

# Verify audit events are present for:
# - LOGIN_ATTEMPT
# - LOGIN_FAILURE (from rate limit test)
# - RATE_LIMIT_EXCEEDED
# - CORS_REJECTED (if tested)
```

**Expected Results:**
- [ ] Audit log file exists at `logs/audit.log`
- [ ] Audit events are in JSON format
- [ ] All expected event types are present
- [ ] Events contain timestamp, action, username, clientIp, result
- [ ] No sensitive data (passwords, TOTP codes) in logs

5. **Successful Authentication Flow:**
```bash
# Wait for rate limit window to reset (or use different IP)
# Attempt login with correct credentials

curl -X POST http://localhost:8080/api/auth/login \
  -H "Origin: http://localhost:3000" \
  -H "Content-Type: application/json" \
  -d '{"username":"validuser","password":"correctpass","totpCode":"<valid-totp>"}' \
  -v

# Expected: HTTP 200 OK
# Expected: JWT token in response
# Expected: LOGIN_SUCCESS in audit log
```

**Expected Results:**
- [ ] Login succeeds with correct credentials
- [ ] JWT token returned
- [ ] LOGIN_SUCCESS event in audit log
- [ ] TOTP code validated correctly (decryption worked)


#### Test Scenario 2: Account Activation Flow

**Test Steps:**

1. **Create invitation token** (use existing admin endpoint)
2. **Activate account with invitation:**
```bash
curl -X POST http://localhost:8080/api/auth/activate \
  -H "Origin: http://localhost:3000" \
  -H "Content-Type: application/json" \
  -d '{"token":"<invitation-token>","username":"newuser","password":"newpass"}' \
  -v

# Expected: HTTP 200 OK
# Expected: Account created with encrypted TOTP secret
```

3. **Verify audit log:**
```bash
tail -n 10 logs/audit.log | jq 'select(.action=="ACCOUNT_ACTIVATION")'

# Verify:
# - ACCOUNT_ACTIVATION event present
# - Contains username and timestamp
# - Contains invitation token ID (not full token)
```

**Expected Results:**
- [ ] Account activation succeeds
- [ ] TOTP secret is encrypted in database
- [ ] ACCOUNT_ACTIVATION event in audit log
- [ ] Rate limiting applies to activation endpoint

### 15.2: API Compatibility Property Test

#### Objective
Verify that security hardening does not break existing API contracts.

**Test Steps:**

1. **Compare API responses before/after security hardening:**

```bash
# Successful login response format
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"pass","totpCode":"123456"}' \
  | jq .

# Verify response structure unchanged:
# {
#   "token": "jwt-token-here",
#   "expiresIn": 3600,
#   "user": { ... }
# }
```

2. **Verify JWT token format unchanged:**
```bash
# Decode JWT token (use jwt.io or jwt-cli)
echo "<jwt-token>" | jwt decode

# Verify claims structure unchanged:
# - sub (subject)
# - exp (expiration)
# - iat (issued at)
# - roles
```

3. **Verify error response formats:**
```bash
# Authentication failure
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"wrong","totpCode":"000000"}' \
  | jq .

# Expected format unchanged:
# {
#   "error": "Authentication failed",
#   "message": "Invalid credentials"
# }

# Rate limit exceeded
# (Make 6 requests to trigger rate limit)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"wrong","totpCode":"000000"}' \
  | jq .

# Expected: HTTP 429 with standard error format
```

**Expected Results:**
- [ ] Login response format unchanged
- [ ] JWT token structure unchanged
- [ ] Error response formats unchanged
- [ ] HTTP status codes appropriate (200, 401, 429)
- [ ] Existing clients can parse responses

### 15.3: Error Scenario Integration Tests

#### Test Scenario 1: Redis Unavailable (Rate Limiting Fails Open)

**Setup:**
```bash
# Stop Redis
docker stop abrolhos-redis
```

**Test Steps:**
```bash
# Attempt login (should succeed despite Redis being down)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"pass","totpCode":"<valid-totp>"}' \
  -v

# Expected: HTTP 200 OK (rate limiting fails open)
# Expected: Warning in application logs about Redis connection
```

**Verify:**
```bash
# Check application logs for Redis warnings
tail -n 50 logs/application.log | grep -i redis

# Expected: Warning about Redis connection failure
# Expected: Message indicating rate limiting is disabled
```

**Cleanup:**
```bash
# Restart Redis
docker start abrolhos-redis
```

**Expected Results:**
- [ ] Login succeeds even with Redis down
- [ ] Warning logged about Redis failure
- [ ] Application remains available (fail open)
- [ ] Rate limiting resumes when Redis reconnects

#### Test Scenario 2: Encryption Key Rotation

**Setup:**
```bash
# Generate new encryption key
kotlin scripts/GenerateEncryptionKey.kt

# Add old key to SECURITY_ENCRYPTION_OLD_KEYS
# Set new key as SECURITY_ENCRYPTION_KEY
# Restart application
```

**Test Steps:**
```bash
# Authenticate user with TOTP secret encrypted with old key
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"olduser","password":"pass","totpCode":"<valid-totp>"}' \
  -v

# Expected: HTTP 200 OK (old key used for decryption)

# Create new user (will use new key for encryption)
curl -X POST http://localhost:8080/api/auth/activate \
  -H "Content-Type: application/json" \
  -d '{"token":"<token>","username":"newuser","password":"pass"}' \
  -v

# Authenticate new user
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","password":"pass","totpCode":"<valid-totp>"}' \
  -v

# Expected: HTTP 200 OK (new key used for decryption)
```

**Expected Results:**
- [ ] Users with old encrypted secrets can still authenticate
- [ ] New users get secrets encrypted with new key
- [ ] No authentication failures during key rotation
- [ ] Application logs key rotation events


#### Test Scenario 3: CORS Rejection

**Test Steps:**
```bash
# Request from unauthorized origin
curl -X POST http://localhost:8080/api/auth/login \
  -H "Origin: http://malicious.com" \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"pass","totpCode":"123456"}' \
  -v

# Expected: No CORS headers in response
# Expected: Browser would block the response
```

**Verify Audit Log:**
```bash
tail -n 20 logs/audit.log | jq 'select(.action=="CORS_REJECTED")'

# Expected: CORS_REJECTED event with:
# - Rejected origin
# - Timestamp
# - Endpoint attempted
```

**Expected Results:**
- [ ] Unauthorized origin does not receive CORS headers
- [ ] CORS_REJECTED event in audit log
- [ ] Request is processed but response blocked by browser

#### Test Scenario 4: Invalid Encryption Key

**Setup:**
```bash
# Set invalid encryption key in environment
export SECURITY_ENCRYPTION_KEY="invalid-key"

# Attempt to start application
./gradlew bootRun
```

**Expected Results:**
- [ ] Application fails to start
- [ ] Clear error message about invalid encryption key
- [ ] Error indicates key must be Base64-encoded 256-bit key

#### Test Scenario 5: Missing Required Configuration

**Test Steps:**
```bash
# Remove required configuration
unset SECURITY_CORS_ALLOWED_ORIGINS

# Attempt to start application
./gradlew bootRun
```

**Expected Results:**
- [ ] Application fails to start
- [ ] Clear error message about missing CORS configuration
- [ ] Error indicates which configuration is missing

### End-to-End Testing Completion Criteria

- [ ] All authentication flows work with security features enabled
- [ ] CORS protection verified (allowed and rejected origins)
- [ ] Rate limiting enforced correctly (429 after limit exceeded)
- [ ] TOTP secrets encrypted in database
- [ ] Audit events logged for all security actions
- [ ] API compatibility maintained (response formats unchanged)
- [ ] Error scenarios handled gracefully (Redis down, key rotation)
- [ ] Configuration validation works (fails fast on invalid config)


---

## Task 16: Final Checkpoint - Production Readiness Validation

### Objective
Verify that all security features are production-ready and meet performance requirements.

### Prerequisites
- Task 14 and Task 15 completed successfully
- Application running with production-like configuration
- Load testing tools available (optional: Apache Bench, JMeter, or k6)

### Procedure

#### Step 1: Configuration Review

**Verify Production Configuration:**

```bash
# Check that production config does not use wildcards
grep SECURITY_CORS_ALLOWED_ORIGINS .env.production

# Expected: Specific domains only, no "*"
# Example: https://app.example.com,https://admin.example.com

# Verify encryption key is from secrets manager (not hardcoded)
grep SECURITY_ENCRYPTION_KEY .env.production

# Expected: Reference to secrets manager, not actual key
# Example: ${AWS_SECRETS_MANAGER:encryption-key}

# Verify rate limiting is appropriately configured
grep SECURITY_RATE_LIMIT .env.production

# Expected: Conservative limits for production
# Example: MAX_REQUESTS=5, WINDOW_MINUTES=15
```

**Configuration Checklist:**
- [ ] CORS origins are specific domains (no wildcards)
- [ ] Encryption key stored in secrets manager
- [ ] Rate limiting configured appropriately
- [ ] Redis connection uses authentication
- [ ] Redis connection uses TLS/SSL (if required)
- [ ] Audit log retention set to 90 days

#### Step 2: Performance Validation

**Test 1: Encryption Performance**

```bash
# Run encryption performance test
./gradlew test --tests "*EncryptionServiceTest.testEncryptionPerformance"

# Verify: All encryption operations < 10ms
```

**Test 2: Rate Limiting Overhead**

```bash
# Measure request latency with rate limiting
# Make 100 requests and measure average response time

for i in {1..100}; do
  curl -w "%{time_total}\n" -o /dev/null -s \
    -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"user","password":"pass","totpCode":"123456"}'
done | awk '{sum+=$1; count++} END {print "Average:", sum/count, "seconds"}'

# Expected: Average < 0.015 seconds (< 15ms overhead)
```

**Test 3: Audit Logging Performance**

```bash
# Verify audit logging is async (non-blocking)
# Check application logs for async appender confirmation

grep "ASYNC_AUDIT" logs/application.log

# Expected: Async appender configured and active
```

**Performance Checklist:**
- [ ] Encryption operations < 10ms
- [ ] Rate limiting overhead < 5ms per request
- [ ] CORS validation overhead < 1ms per request
- [ ] Audit logging is async (< 1ms overhead)
- [ ] Overall security overhead < 20ms per request

#### Step 3: Security Feature Verification

**Verify All Security Features Enabled:**

```bash
# Check application startup logs
tail -n 100 logs/application.log | grep -i security

# Expected messages:
# - "Security configuration loaded"
# - "CORS allowed origins: <domains>"
# - "Rate limit max requests: 5"
# - "Encryption key configured: true"
# - "Audit logging enabled"
```

**Security Features Checklist:**
- [ ] CORS hardening active (specific origins only)
- [ ] Rate limiting active on /api/auth/* endpoints
- [ ] TOTP secrets encrypted at rest
- [ ] Audit logging active (events written to audit.log)
- [ ] All security features logged at startup

#### Step 4: Audit Log Review

**Verify Audit Log Completeness:**

```bash
# Review audit log for all event types
cat logs/audit.log | jq -r '.action' | sort | uniq

# Expected event types:
# - LOGIN_ATTEMPT
# - LOGIN_SUCCESS
# - LOGIN_FAILURE
# - ACCOUNT_ACTIVATION
# - RATE_LIMIT_EXCEEDED
# - CORS_REJECTED (if tested)
# - TOKEN_VALIDATION
```

**Verify Audit Log Format:**

```bash
# Check JSON format is valid
cat logs/audit.log | jq . > /dev/null

# Expected: No JSON parsing errors

# Verify required fields present
cat logs/audit.log | jq '{timestamp, action, result, username, clientIp}' | head -n 5

# Expected: All fields present in each event
```

**Audit Log Checklist:**
- [ ] All event types present
- [ ] JSON format valid and parseable
- [ ] Required fields present (timestamp, action, result, username, clientIp)
- [ ] No sensitive data in logs (passwords, TOTP codes, encryption keys)
- [ ] Log rotation configured (90-day retention)
- [ ] Async appender active (non-blocking writes)

#### Step 5: Database Verification

**Verify TOTP Secret Encryption:**

```bash
# Connect to database
docker exec -it abrolhos-postgres psql -U abrolhos -d abrolhos

# Check TOTP secrets are encrypted
SELECT username, 
       LENGTH(totp_secret) as secret_length,
       LEFT(totp_secret, 20) as secret_preview
FROM users 
WHERE totp_secret IS NOT NULL
LIMIT 5;

# Expected:
# - secret_length > 100 (encrypted data is longer)
# - secret_preview shows encrypted format (not base32)
```

**Database Checklist:**
- [ ] All TOTP secrets are encrypted (not plaintext)
- [ ] Encrypted values contain IV and ciphertext
- [ ] Column size adequate for encrypted data (VARCHAR(500))
- [ ] Migration script executed successfully

#### Step 6: Integration Test Review

**Run Full Integration Test Suite:**

```bash
# Run all integration tests
./gradlew test --tests "*IntegrationTest"

# Expected: All integration tests pass
```

**Integration Test Checklist:**
- [ ] UserJpaEncryptionIntegrationTest passes (4 tests)
- [ ] AuditAspectIntegrationTest passes (8 tests)
- [ ] AuditLogConfigurationTest passes (2 tests)
- [ ] All integration tests verify end-to-end functionality


#### Step 7: Production Deployment Readiness

**Pre-Deployment Checklist:**

**Configuration:**
- [ ] Encryption key generated and stored in secrets manager
- [ ] CORS origins configured for production domains
- [ ] Rate limiting configured appropriately
- [ ] Redis connection secured (authentication + TLS)
- [ ] All required environment variables set

**Security:**
- [ ] No wildcards in CORS configuration
- [ ] Encryption key never committed to version control
- [ ] Audit logs configured for aggregation system
- [ ] Alerts configured for rate limit violations
- [ ] Alerts configured for encryption errors

**Testing:**
- [ ] All 137+ automated tests pass
- [ ] All property-based tests pass (100 iterations each)
- [ ] All integration tests pass
- [ ] End-to-end testing completed successfully
- [ ] Performance requirements met (< 10ms overhead)

**Documentation:**
- [ ] Configuration guide reviewed (`docs/security-configuration.md`)
- [ ] Deployment checklist reviewed
- [ ] Incident response procedures documented
- [ ] Key rotation procedure documented

**Monitoring:**
- [ ] Log aggregation configured for audit logs
- [ ] Alerts configured for security events
- [ ] Dashboards created for rate limiting metrics
- [ ] Dashboards created for encryption performance

#### Step 8: Final Validation Questions

Answer these questions to confirm production readiness:

1. **Are all security features enabled and functioning?**
   - [ ] Yes - All features verified in Steps 1-6
   - [ ] No - Review failures and address issues

2. **Do all automated tests pass?**
   - [ ] Yes - 137+ tests passing
   - [ ] No - Fix failing tests before deployment

3. **Are performance requirements met?**
   - [ ] Yes - All operations < 10ms overhead
   - [ ] No - Optimize slow operations

4. **Is configuration production-ready?**
   - [ ] Yes - No wildcards, keys in secrets manager
   - [ ] No - Update configuration

5. **Are audit logs complete and parseable?**
   - [ ] Yes - All events logged in valid JSON
   - [ ] No - Fix audit logging issues

6. **Is the database migration ready?**
   - [ ] Yes - All secrets encrypted
   - [ ] No - Run migration script

7. **Are monitoring and alerts configured?**
   - [ ] Yes - Logs aggregated, alerts active
   - [ ] No - Configure monitoring

8. **Is documentation complete?**
   - [ ] Yes - All guides and procedures documented
   - [ ] No - Complete documentation

### Final Checkpoint Completion Criteria

- [ ] All security features verified and functioning
- [ ] All automated tests pass (137+ tests)
- [ ] Performance requirements met (< 10ms overhead)
- [ ] Configuration is production-ready
- [ ] Audit logs complete and parseable
- [ ] Database migration successful
- [ ] Monitoring and alerts configured
- [ ] Documentation complete
- [ ] All validation questions answered "Yes"

### Sign-Off

**Completed By:** ___________________________

**Date:** ___________________________

**Notes:**
_____________________________________________
_____________________________________________
_____________________________________________


---

## Appendix: Quick Reference Commands

### Start Services
```bash
# Start Redis
docker run -d -p 6379:6379 --name abrolhos-redis redis:7-alpine

# Start application
./gradlew bootRun
```

### Run Tests
```bash
# All tests
./gradlew test

# Specific component
./gradlew test --tests "*CorsConfigTest"
./gradlew test --tests "*EncryptionServiceTest"
./gradlew test --tests "*RateLimitServiceTest"
./gradlew test --tests "*AuditLoggerTest"

# Property-based tests only
./gradlew test --tests "*PropertyTest"

# Integration tests only
./gradlew test --tests "*IntegrationTest"
```

### Check Logs
```bash
# Application logs
tail -f logs/application.log

# Audit logs
tail -f logs/audit.log

# Audit logs (JSON formatted)
tail -f logs/audit.log | jq .

# Filter audit logs by action
cat logs/audit.log | jq 'select(.action=="LOGIN_FAILURE")'
```

### Database Queries
```bash
# Connect to database
docker exec -it abrolhos-postgres psql -U abrolhos -d abrolhos

# Check encrypted secrets
SELECT username, totp_secret FROM users LIMIT 5;

# Count users with encrypted secrets
SELECT COUNT(*) FROM users WHERE totp_secret IS NOT NULL;
```

### Generate Encryption Key
```bash
kotlin scripts/GenerateEncryptionKey.kt
```

### Test API Endpoints
```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Origin: http://localhost:3000" \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"pass","totpCode":"123456"}'

# Activate account
curl -X POST http://localhost:8080/api/auth/activate \
  -H "Origin: http://localhost:3000" \
  -H "Content-Type: application/json" \
  -d '{"token":"<token>","username":"user","password":"pass"}'
```

---

## Document Information

**Version:** 1.0  
**Last Updated:** February 15, 2026  
**Related Documents:**
- `docs/requirements.md` - Security hardening requirements
- `docs/design.md` - Security hardening design
- `docs/tasks.md` - Implementation task list
- `docs/security-configuration.md` - Configuration guide
- `docs/security-hardening-completion-summary.md` - Implementation summary

**Purpose:** This document provides step-by-step manual testing procedures for Tasks 14, 15, and 16 of the security hardening implementation. It ensures all security features are validated before production deployment.

**Usage:** Follow the procedures in order (Task 14 → Task 15 → Task 16) to systematically validate the security hardening implementation. Check off each item as you complete it.
