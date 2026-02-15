# Design Document: Security Hardening

## Overview

This design document specifies the technical implementation for four critical security improvements to the Abrolhos application: environment-specific CORS configuration, rate limiting on authentication endpoints, TOTP secret encryption at rest, and comprehensive audit logging.

The design follows the existing layered/hexagonal architecture with Domain-Driven Design principles. Security components are implemented as cross-cutting concerns that integrate seamlessly with the existing authentication flow without disrupting the domain model.

### Key Design Principles

1. **Security by Default**: All security features are mandatory and fail-closed
2. **Performance First**: Security overhead must be minimal (< 10ms per operation)
3. **Graceful Degradation**: External dependencies (Redis) fail open with logging
4. **Transparent Integration**: Security features integrate via Spring interceptors and JPA converters
5. **Testability**: All components support property-based testing

## Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     Web Layer (Controllers)                  │
│                    AuthController, etc.                      │
└────────────┬────────────────────────────────┬───────────────┘
             │                                │
             ▼                                ▼
┌────────────────────────┐      ┌────────────────────────────┐
│   Rate Limit Filter    │      │   CORS Configuration       │
│   (Spring Interceptor) │      │   (SecurityConfig)         │
└────────────┬───────────┘      └────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────┐
│                   Application Layer (Services)               │
│              AuthService, TotpService, etc.                  │
└────────────┬────────────────────────────────┬───────────────┘
             │                                │
             ▼                                ▼
┌────────────────────────┐      ┌────────────────────────────┐
│   Audit Logger         │      │   Encryption Service       │
│   (Aspect/Interceptor) │      │   (JPA Converter)          │
└────────────────────────┘      └────────────┬───────────────┘
                                              │
                                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Persistence Layer (JPA)                    │
│                  UserRepository, etc.                        │
└─────────────────────────────────────────────────────────────┘
```

### Integration Points

1. **CORS Configuration**: Replaces existing `corsConfigurationSource()` in `SecurityConfig`
2. **Rate Limiting**: Implemented as Spring `HandlerInterceptor` registered for auth endpoints
3. **Encryption**: Implemented as JPA `AttributeConverter` for `TotpSecret` field
4. **Audit Logging**: Implemented as Spring AOP `@Aspect` intercepting auth methods

## Components and Interfaces

### 1. CORS Configuration

**Location**: `application/config/CorsConfig.kt`

**Purpose**: Replace wildcard CORS with environment-specific allowed origins

**Implementation**:

```kotlin
@Configuration
class CorsConfig(
    @Value("\${security.cors.allowed-origins}")
    private val allowedOrigins: String,
    
    @Value("\${spring.profiles.active:default}")
    private val activeProfile: String
) {
    
    private val logger = LoggerFactory.getLogger(CorsConfig::class.java)
    
    @PostConstruct
    fun validateConfiguration() {
        // Requirement 1.1: Reject wildcard in production
        if (activeProfile == "prod" && allowedOrigins.contains("*")) {
            throw IllegalStateException(
                "Wildcard CORS origins are not allowed in production"
            )
        }
        
        // Requirement 1.7: Validate origin URLs
        parseOrigins().forEach { origin ->
            try {
                URL(origin)
            } catch (e: MalformedURLException) {
                throw IllegalStateException(
                    "Invalid origin URL in ALLOWED_ORIGINS: $origin", e
                )
            }
        }
        
        logger.info("CORS configured with origins: ${parseOrigins()}")
    }
    
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        
        // Requirement 1.2, 1.8: Parse comma-separated origins
        configuration.allowedOrigins = parseOrigins()
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L
        
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        
        return source
    }
    
    private fun parseOrigins(): List<String> {
        return allowedOrigins.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }
}
```

**Configuration**:
- Environment variable: `SECURITY_CORS_ALLOWED_ORIGINS`
- Format: Comma-separated URLs (e.g., `https://app.example.com,https://admin.example.com`)
- Validation: Startup fails if wildcard in production or malformed URLs

### 2. Rate Limiting

**Location**: `infrastructure/web/filters/RateLimitFilter.kt`

**Purpose**: Prevent brute force attacks on authentication endpoints using Bucket4j and Redis

**Dependencies**:
```kotlin
// build.gradle.kts additions
implementation("com.bucket4j:bucket4j-core:8.7.0")
implementation("com.bucket4j:bucket4j-redis:8.7.0")
implementation("org.springframework.boot:spring-boot-starter-data-redis")
implementation("io.lettuce:lettuce-core")
```

**Implementation**:

```kotlin
@Component
class RateLimitFilter(
    private val rateLimitService: RateLimitService,
    private val auditLogger: AuditLogger
) : HandlerInterceptor {
    
    private val logger = LoggerFactory.getLogger(RateLimitFilter::class.java)
    
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val clientId = extractClientIdentifier(request)
        val path = request.requestURI
        
        // Only apply to authentication endpoints
        if (!isAuthEndpoint(path)) {
            return true
        }
        
        return try {
            val result = rateLimitService.tryConsume(clientId, path)
            
            if (result.isAllowed) {
                // Requirement 2.4, 2.5, 2.6, 2.7: Add rate limit headers
                response.addHeader("X-RateLimit-Limit", result.limit.toString())
                response.addHeader("X-RateLimit-Remaining", result.remaining.toString())
                response.addHeader("X-RateLimit-Reset", result.resetTime.toString())
                true
            } else {
                // Requirement 2.3: Return 429
                response.status = HttpStatus.TOO_MANY_REQUESTS.value()
                response.addHeader("X-RateLimit-Limit", result.limit.toString())
                response.addHeader("X-RateLimit-Remaining", "0")
                response.addHeader("X-RateLimit-Reset", result.resetTime.toString())
                response.addHeader("Retry-After", result.retryAfterSeconds.toString())
                response.contentType = "application/json"
                response.writer.write(
                    """{"error":"Too many requests","retryAfter":${result.retryAfterSeconds}}"""
                )
                
                // Requirement 4.5: Audit log rate limit event
                auditLogger.logRateLimitExceeded(clientId, path)
                
                false
            }
        } catch (e: Exception) {
            // Requirement 2.12: Fail open if Redis unavailable
            logger.warn("Rate limiting unavailable, allowing request: ${e.message}")
            true
        }
    }
    
    private fun extractClientIdentifier(request: HttpServletRequest): String {
        // Requirement 2.1, 2.2: Use IP as client identifier
        return request.getHeader("X-Forwarded-For")?.split(",")?.first()?.trim()
            ?: request.remoteAddr
    }
    
    private fun isAuthEndpoint(path: String): Boolean {
        return path == "/api/auth/login" || path == "/api/auth/activate"
    }
}

@Service
class RateLimitService(
    private val redisTemplate: RedisTemplate<String, String>,
    @Value("\${security.rate-limit.max-requests:5}")
    private val maxRequests: Int,
    
    @Value("\${security.rate-limit.window-minutes:15}")
    private val windowMinutes: Int
) {
    
    private val logger = LoggerFactory.getLogger(RateLimitService::class.java)
    
    data class RateLimitResult(
        val isAllowed: Boolean,
        val limit: Int,
        val remaining: Int,
        val resetTime: Long,
        val retryAfterSeconds: Long
    )
    
    fun tryConsume(clientId: String, endpoint: String): RateLimitResult {
        val key = "rate_limit:$endpoint:$clientId"
        val now = System.currentTimeMillis()
        val windowStart = now - (windowMinutes * 60 * 1000)
        
        // Requirement 2.8: Use Redis for distributed rate limiting
        val operations = redisTemplate.opsForZSet()
        
        // Remove old entries outside the window
        operations.removeRangeByScore(key, 0.0, windowStart.toDouble())
        
        // Count requests in current window
        val count = operations.count(key, windowStart.toDouble(), now.toDouble()) ?: 0
        
        val resetTime = now + (windowMinutes * 60 * 1000)
        
        return if (count < maxRequests) {
            // Requirement 2.1, 2.2: Track request
            operations.add(key, now.toString(), now.toDouble())
            redisTemplate.expire(key, windowMinutes.toLong(), TimeUnit.MINUTES)
            
            RateLimitResult(
                isAllowed = true,
                limit = maxRequests,
                remaining = (maxRequests - count - 1).toInt(),
                resetTime = resetTime,
                retryAfterSeconds = 0
            )
        } else {
            // Requirement 2.9: Apply exponential backoff
            val backoffMultiplier = calculateBackoffMultiplier(count.toInt())
            val extendedResetTime = resetTime + (windowMinutes * 60 * 1000 * backoffMultiplier)
            
            RateLimitResult(
                isAllowed = false,
                limit = maxRequests,
                remaining = 0,
                resetTime = extendedResetTime,
                retryAfterSeconds = ((extendedResetTime - now) / 1000)
            )
        }
    }
    
    private fun calculateBackoffMultiplier(attemptCount: Int): Int {
        // Exponential backoff: 1x, 2x, 4x, 8x (capped at 8x)
        return minOf(1 shl (attemptCount - maxRequests), 8)
    }
}
```

**Configuration**:
- `SECURITY_RATE_LIMIT_MAX_REQUESTS`: Default 5
- `SECURITY_RATE_LIMIT_WINDOW_MINUTES`: Default 15
- `SPRING_DATA_REDIS_HOST`: Redis host
- `SPRING_DATA_REDIS_PORT`: Redis port

### 3. TOTP Secret Encryption

**Location**: `infrastructure/persistence/converters/TotpSecretConverter.kt`

**Purpose**: Transparently encrypt/decrypt TOTP secrets using AES-256-GCM

**Implementation**:

```kotlin
@Converter
class TotpSecretConverter(
    private val encryptionService: EncryptionService
) : AttributeConverter<TotpSecret?, String?> {
    
    override fun convertToDatabaseColumn(attribute: TotpSecret?): String? {
        // Requirement 3.1: Encrypt when persisting
        return attribute?.let { encryptionService.encrypt(it.value) }
    }
    
    override fun convertToEntityAttribute(dbData: String?): TotpSecret? {
        // Requirement 3.2: Decrypt when reading
        return dbData?.let { 
            val decrypted = encryptionService.decrypt(it)
            TotpSecret(decrypted)
        }
    }
}

@Service
class EncryptionService(
    @Value("\${security.encryption.key}")
    private val encryptionKeyBase64: String,
    
    @Value("\${security.encryption.old-keys:}")
    private val oldKeysBase64: String
) {
    
    private val logger = LoggerFactory.getLogger(EncryptionService::class.java)
    private val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    private val secureRandom = SecureRandom()
    
    private lateinit var currentKey: SecretKey
    private lateinit var oldKeys: List<SecretKey>
    
    companion object {
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val KEY_SIZE_BITS = 256
    }
    
    @PostConstruct
    fun initialize() {
        // Requirement 3.3, 3.4, 3.5: Validate encryption key
        try {
            val keyBytes = Base64.getDecoder().decode(encryptionKeyBase64)
            require(keyBytes.size * 8 >= KEY_SIZE_BITS) {
                "Encryption key must be at least $KEY_SIZE_BITS bits"
            }
            currentKey = SecretKeySpec(keyBytes, "AES")
            
            // Requirement 3.9: Support key rotation
            oldKeys = if (oldKeysBase64.isNotBlank()) {
                oldKeysBase64.split(",")
                    .map { Base64.getDecoder().decode(it.trim()) }
                    .map { SecretKeySpec(it, "AES") }
            } else {
                emptyList()
            }
            
            logger.info("Encryption service initialized with AES-256-GCM")
        } catch (e: Exception) {
            throw IllegalStateException(
                "Failed to initialize encryption service: ${e.message}", e
            )
        }
    }
    
    fun encrypt(plaintext: String): String {
        val startTime = System.currentTimeMillis()
        
        try {
            // Requirement 3.6: Generate unique IV
            val iv = ByteArray(GCM_IV_LENGTH)
            secureRandom.nextBytes(iv)
            val ivSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            
            // Requirement 3.1: Encrypt using AES-256-GCM
            cipher.init(Cipher.ENCRYPT_MODE, currentKey, ivSpec)
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            
            // Requirement 3.7: Store IV with encrypted data
            val combined = ByteArray(iv.size + ciphertext.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
            
            val result = Base64.getEncoder().encodeToString(combined)
            
            // Requirement 3.11: Performance < 10ms
            val duration = System.currentTimeMillis() - startTime
            if (duration > 10) {
                logger.warn("Encryption took {}ms (exceeds 10ms threshold)", duration)
            }
            
            return result
        } catch (e: Exception) {
            logger.error("Encryption failed: ${e.message}", e)
            throw EncryptionException("Failed to encrypt data", e)
        }
    }
    
    fun decrypt(ciphertext: String): String {
        val startTime = System.currentTimeMillis()
        
        try {
            val combined = Base64.getDecoder().decode(ciphertext)
            
            // Requirement 3.7: Extract IV from stored data
            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
            val ivSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            
            // Requirement 3.9, 3.10: Try current key, then old keys
            val keys = listOf(currentKey) + oldKeys
            
            for (key in keys) {
                try {
                    cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
                    val plaintext = cipher.doFinal(encrypted)
                    
                    // Requirement 3.12: Performance < 10ms
                    val duration = System.currentTimeMillis() - startTime
                    if (duration > 10) {
                        logger.warn("Decryption took {}ms (exceeds 10ms threshold)", duration)
                    }
                    
                    return String(plaintext, Charsets.UTF_8)
                } catch (e: AEADBadTagException) {
                    // Wrong key, try next
                    continue
                }
            }
            
            // Requirement 3.8: Throw descriptive exception
            throw EncryptionException("Failed to decrypt data with any available key")
        } catch (e: EncryptionException) {
            throw e
        } catch (e: Exception) {
            logger.error("Decryption failed: ${e.message}", e)
            throw EncryptionException("Failed to decrypt data", e)
        }
    }
}

class EncryptionException(message: String, cause: Throwable? = null) : 
    RuntimeException(message, cause)
```

**Database Schema Update**:

```sql
-- Flyway migration: V4__encrypt_totp_secrets.sql
-- Update totp_secret column to store encrypted data (longer length needed)
ALTER TABLE users ALTER COLUMN totp_secret TYPE VARCHAR(500);

-- Add comment documenting encryption
COMMENT ON COLUMN users.totp_secret IS 'AES-256-GCM encrypted TOTP secret (Base64 encoded with IV)';
```

**Configuration**:
- `SECURITY_ENCRYPTION_KEY`: Base64-encoded 256-bit key (required)
- `SECURITY_ENCRYPTION_OLD_KEYS`: Comma-separated Base64-encoded keys for rotation (optional)

**Key Generation Script**:

```kotlin
// scripts/GenerateEncryptionKey.kt
fun main() {
    val keyGen = KeyGenerator.getInstance("AES")
    keyGen.init(256)
    val key = keyGen.generateKey()
    val base64Key = Base64.getEncoder().encodeToString(key.encoded)
    println("Generated AES-256 key (add to environment):")
    println("SECURITY_ENCRYPTION_KEY=$base64Key")
}
```

### 4. Audit Logging

**Location**: `application/audit/AuditLogger.kt`

**Purpose**: Log all security events in structured JSON format

**Implementation**:

```kotlin
data class AuditEvent(
    val timestamp: String,
    val action: String,
    val result: String,
    val username: String?,
    val clientIp: String,
    val userAgent: String,
    val details: Map<String, Any> = emptyMap()
)

@Component
class AuditLogger {
    
    // Requirement 4.16: Separate audit log file
    private val auditLog = LoggerFactory.getLogger("AUDIT")
    
    fun logLoginAttempt(username: String, clientIp: String, userAgent: String) {
        // Requirement 4.1
        logEvent(
            action = "LOGIN_ATTEMPT",
            result = "PENDING",
            username = username,
            clientIp = clientIp,
            userAgent = userAgent
        )
    }
    
    fun logLoginSuccess(username: String, clientIp: String, userAgent: String) {
        // Requirement 4.2
        logEvent(
            action = "LOGIN_SUCCESS",
            result = "SUCCESS",
            username = username,
            clientIp = clientIp,
            userAgent = userAgent
        )
    }
    
    fun logLoginFailure(
        username: String, 
        clientIp: String, 
        userAgent: String, 
        reason: String
    ) {
        // Requirement 4.3
        logEvent(
            action = "LOGIN_FAILURE",
            result = "FAILURE",
            username = username,
            clientIp = clientIp,
            userAgent = userAgent,
            details = mapOf("reason" to reason)
        )
    }
    
    fun logAccountActivation(username: String, clientIp: String, userAgent: String) {
        // Requirement 4.4
        logEvent(
            action = "ACCOUNT_ACTIVATION",
            result = "SUCCESS",
            username = username,
            clientIp = clientIp,
            userAgent = userAgent
        )
    }
    
    fun logRateLimitExceeded(clientIp: String, endpoint: String) {
        // Requirement 4.5
        logEvent(
            action = "RATE_LIMIT_EXCEEDED",
            result = "BLOCKED",
            username = null,
            clientIp = clientIp,
            userAgent = "",
            details = mapOf("endpoint" to endpoint)
        )
    }
    
    fun logCorsRejected(origin: String, clientIp: String) {
        // Requirement 4.6
        logEvent(
            action = "CORS_REJECTED",
            result = "BLOCKED",
            username = null,
            clientIp = clientIp,
            userAgent = "",
            details = mapOf("origin" to origin)
        )
    }
    
    fun logTokenValidation(username: String, clientIp: String, success: Boolean) {
        // Requirement 4.7, 4.8
        logEvent(
            action = if (success) "TOKEN_VALIDATION" else "TOKEN_VALIDATION_FAILURE",
            result = if (success) "SUCCESS" else "FAILURE",
            username = username,
            clientIp = clientIp,
            userAgent = ""
        )
    }
    
    private fun logEvent(
        action: String,
        result: String,
        username: String?,
        clientIp: String,
        userAgent: String,
        details: Map<String, Any> = emptyMap()
    ) {
        // Requirement 4.9-4.14: Include all required fields
        val event = AuditEvent(
            timestamp = OffsetDateTime.now().toString(), // ISO 8601 with timezone
            action = action,
            result = result,
            username = username,
            clientIp = clientIp,
            userAgent = userAgent,
            details = details
        )
        
        // Requirement 4.15: Structured JSON format
        // Requirement 4.20: Async logging (Logback async appender)
        val json = ObjectMapper().writeValueAsString(event)
        auditLog.info(json)
    }
}

@Aspect
@Component
class AuditAspect(
    private val auditLogger: AuditLogger
) {
    
    @Around("execution(* br.dev.demoraes.abrolhos.application.services.AuthService.login(..))")
    fun auditLogin(joinPoint: ProceedingJoinPoint): Any? {
        val args = joinPoint.args
        val username = (args[0] as Username).value
        val request = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes
        val httpRequest = request.request
        val clientIp = extractClientIp(httpRequest)
        val userAgent = httpRequest.getHeader("User-Agent") ?: ""
        
        auditLogger.logLoginAttempt(username, clientIp, userAgent)
        
        return try {
            val result = joinPoint.proceed()
            auditLogger.logLoginSuccess(username, clientIp, userAgent)
            result
        } catch (e: Exception) {
            auditLogger.logLoginFailure(username, clientIp, userAgent, e.message ?: "Unknown error")
            throw e
        }
    }
    
    @Around("execution(* br.dev.demoraes.abrolhos.application.services.AuthService.activateAccount(..))")
    fun auditActivation(joinPoint: ProceedingJoinPoint): Any? {
        val request = RequestContextHolder.getRequestAttributes() as ServletRequestAttributes
        val httpRequest = request.request
        val clientIp = extractClientIp(httpRequest)
        val userAgent = httpRequest.getHeader("User-Agent") ?: ""
        
        return try {
            val result = joinPoint.proceed()
            // Extract username from result or context
            auditLogger.logAccountActivation("user", clientIp, userAgent)
            result
        } catch (e: Exception) {
            throw e
        }
    }
    
    private fun extractClientIp(request: HttpServletRequest): String {
        return request.getHeader("X-Forwarded-For")?.split(",")?.first()?.trim()
            ?: request.remoteAddr
    }
}
```

**Logback Configuration** (`src/main/resources/logback-spring.xml`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Existing appenders... -->
    
    <!-- Requirement 4.16, 4.19: Separate audit log with rotation -->
    <appender name="AUDIT_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/audit.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/audit.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>90</maxHistory> <!-- 90-day retention -->
        </rollingPolicy>
        <encoder>
            <pattern>%msg%n</pattern> <!-- Raw JSON, no extra formatting -->
        </encoder>
    </appender>
    
    <!-- Requirement 4.20: Async appender for non-blocking writes -->
    <appender name="ASYNC_AUDIT" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="AUDIT_FILE" />
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
    </appender>
    
    <logger name="AUDIT" level="INFO" additivity="false">
        <appender-ref ref="ASYNC_AUDIT" />
    </logger>
</configuration>
```

## Data Models

### Encrypted TOTP Secret Storage

**Database Column**:
```sql
totp_secret VARCHAR(500) -- Stores: Base64(IV || AES-GCM-Ciphertext)
```

**Format**: `Base64(12-byte-IV || encrypted-secret || 16-byte-auth-tag)`

**Example**:
- Original secret: `JBSWY3DPEHPK3PXP` (16 chars)
- Encrypted: `rQw7K...` (approx 88 chars Base64)

### Audit Event Schema

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

### Rate Limit Redis Schema

**Key Pattern**: `rate_limit:{endpoint}:{clientIp}`

**Data Structure**: Sorted Set (ZSET)
- Score: Timestamp (milliseconds)
- Member: Request ID (timestamp as string)

**Example**:
```
rate_limit:/api/auth/login:192.168.1.100
  1705315845123 -> "1705315845123"
  1705315846234 -> "1705315846234"
  ...
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

