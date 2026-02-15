# Security Configuration Guide

This document describes the security hardening configuration for the Abrolhos application.

## Overview

The security hardening implementation includes four main features:

1. **CORS Configuration Hardening** - Environment-specific allowed origins
2. **Rate Limiting** - Protection against brute force attacks
3. **TOTP Secret Encryption** - Encryption at rest for authentication secrets
4. **Audit Logging** - Comprehensive security event logging

## Configuration Requirements

### Required Environment Variables

#### CORS Configuration (Requirement 6.1)

```bash
SECURITY_CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com
```

- **Format**: Comma-separated list of URLs
- **Validation**: Must be valid URLs, wildcards (*) not allowed in production
- **Example**: `http://localhost:3000,http://localhost:5173` (development)

#### Rate Limiting Configuration (Requirement 6.2)

```bash
SECURITY_RATE_LIMIT_MAX_REQUESTS=5
SECURITY_RATE_LIMIT_WINDOW_MINUTES=15
```

- **MAX_REQUESTS**: Maximum number of requests allowed within the time window (default: 5)
- **WINDOW_MINUTES**: Time window in minutes for rate limiting (default: 15)
- **Applies to**: `/api/auth/login`, `/api/auth/activate` endpoints

#### Encryption Configuration (Requirement 6.3)

```bash
SECURITY_ENCRYPTION_KEY=<base64-encoded-256-bit-key>
```

- **Format**: Base64-encoded AES-256 key
- **Required**: Yes - application will fail to start without it
- **Generation**: Use `kotlin scripts/GenerateEncryptionKey.kt`
- **Security**: Never commit to version control, use secrets manager in production

#### Redis Configuration (Requirement 2.8)

```bash
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=
```

- **Purpose**: Distributed rate limiting storage
- **Defaults**: localhost:6379 (no password)
- **Production**: Use managed Redis with authentication enabled

### Optional Environment Variables

#### Key Rotation Support

```bash
SECURITY_ENCRYPTION_OLD_KEYS=old_key_1,old_key_2
```

- **Format**: Comma-separated list of Base64-encoded keys
- **Purpose**: Allows decryption of data encrypted with old keys during key rotation
- **Usage**: Add old key here when rotating to a new encryption key

## Setup Instructions

### 1. Generate Encryption Key

```bash
kotlin scripts/GenerateEncryptionKey.kt
```

This will output a secure AES-256 key. Add it to your environment variables.

### 2. Configure Environment Variables

Copy the template and fill in values:

```bash
cp .env.template local.env
# Edit local.env with your values
```

### 3. Start Redis (for Rate Limiting)

Using Docker:

```bash
docker run -d -p 6379:6379 --name abrolhos-redis redis:7-alpine
```

Or use the existing docker-compose setup:

```bash
cd container
docker-compose up -d redis
```

### 4. Verify Configuration

Start the application and check the logs:

```bash
./gradlew bootRun
```

Look for:
```
Security configuration loaded:
  CORS allowed origins: http://localhost:3000,http://localhost:5173
  Rate limit max requests: 5
  Rate limit window: 15 minutes
  Encryption key configured: true
```

## Environment-Specific Configuration

### Development

```bash
SECURITY_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
SECURITY_RATE_LIMIT_MAX_REQUESTS=10
SECURITY_RATE_LIMIT_WINDOW_MINUTES=5
SECURITY_ENCRYPTION_KEY=<generated-key>
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
```

### Production

```bash
SECURITY_CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com
SECURITY_RATE_LIMIT_MAX_REQUESTS=5
SECURITY_RATE_LIMIT_WINDOW_MINUTES=15
SECURITY_ENCRYPTION_KEY=<from-secrets-manager>
SPRING_DATA_REDIS_HOST=redis.production.internal
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=<secure-password>
```

**Production Security Checklist:**
- [ ] Never use wildcard (*) in CORS origins
- [ ] Store encryption key in AWS Secrets Manager or similar
- [ ] Use managed Redis with authentication enabled
- [ ] Enable Redis TLS/SSL encryption
- [ ] Rotate encryption keys periodically
- [ ] Monitor audit logs for security events
- [ ] Set up alerts for rate limit violations

## Validation Rules

The application validates configuration at startup and will fail fast with clear error messages:

### CORS Validation (Requirement 6.4)
- ✓ Origins must be valid URLs
- ✓ Wildcards not allowed in production profile
- ✓ At least one origin must be configured

### Encryption Validation (Requirement 6.5)
- ✓ Key must be Base64-encoded
- ✓ Key must be at least 256 bits (32 bytes)
- ✓ Key must not be empty

### Rate Limit Validation
- ✓ Max requests must be at least 1
- ✓ Window minutes must be at least 1

## Troubleshooting

### Application fails to start with "Encryption key must be configured"

**Solution**: Generate and set the `SECURITY_ENCRYPTION_KEY` environment variable:
```bash
kotlin scripts/GenerateEncryptionKey.kt
# Add output to local.env
```

### Rate limiting not working

**Possible causes:**
1. Redis not running - check with `docker ps` or `redis-cli ping`
2. Redis connection failed - check host/port configuration
3. Redis authentication failed - verify password

**Note**: Rate limiting fails open (allows requests) if Redis is unavailable, with warnings logged.

### CORS errors in browser

**Solution**: Verify the frontend origin is in `SECURITY_CORS_ALLOWED_ORIGINS`:
```bash
# Check current configuration
grep SECURITY_CORS_ALLOWED_ORIGINS local.env

# Add your frontend origin
SECURITY_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
```

## Key Rotation Procedure

When rotating encryption keys:

1. Generate new key: `kotlin scripts/GenerateEncryptionKey.kt`
2. Add current key to `SECURITY_ENCRYPTION_OLD_KEYS`
3. Set new key as `SECURITY_ENCRYPTION_KEY`
4. Deploy application
5. Run data migration to re-encrypt with new key (optional)
6. After migration complete, remove old keys from configuration

Example:
```bash
# Before rotation
SECURITY_ENCRYPTION_KEY=old_key_here

# During rotation
SECURITY_ENCRYPTION_KEY=new_key_here
SECURITY_ENCRYPTION_OLD_KEYS=old_key_here

# After migration (optional)
SECURITY_ENCRYPTION_KEY=new_key_here
# Remove SECURITY_ENCRYPTION_OLD_KEYS
```

## Monitoring and Alerts

### Audit Logs

Security events are logged to `logs/audit.log` in JSON format:

```json
{
  "timestamp": "2024-01-15T10:30:45.123+00:00",
  "action": "LOGIN_SUCCESS",
  "result": "SUCCESS",
  "username": "john_doe",
  "clientIp": "192.168.1.100",
  "userAgent": "Mozilla/5.0...",
  "details": {}
}
```

### Recommended Alerts

1. **Rate Limit Exceeded** - Alert when `RATE_LIMIT_EXCEEDED` events spike
2. **CORS Rejected** - Alert on `CORS_REJECTED` events (potential attack)
3. **Login Failures** - Alert on multiple `LOGIN_FAILURE` events from same IP
4. **Encryption Errors** - Alert on encryption/decryption failures

## References

- Requirements: `.kiro/security-hardening/requirements.md`
- Design: `.kiro/security-hardening/design.md`
- Tasks: `.kiro/security-hardening/tasks.md`
