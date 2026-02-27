# Design Document: Production Readiness Improvements

## Overview

This document outlines the design for production readiness improvements to the Abrolhos backend application. The improvements are organized into three phases: Critical Security, Observability, and Code Quality. The design focuses on a single-instance, self-hosted deployment using VictoriaMetrics for metrics, VictoriaLogs for centralized logging, and Zipkin for distributed tracing.

The implementation will enhance security through JWT secret validation and actuator endpoint protection, improve observability through correlation IDs and distributed tracing, and ensure code quality through TODO resolution and security hardening.

## Architecture

### Current Architecture

The Abrolhos backend is a Spring Boot 3.4 application using Kotlin, structured with clean architecture principles:

- **Application Layer**: Services, configuration, and audit logging
- **Domain Layer**: Entities, exceptions, and repository interfaces
- **Infrastructure Layer**: Web (controllers, filters, handlers), persistence, caching, and monitoring

### Deployment Context

- **Single instance deployment** (no distributed locking needed)
- **Secrets injected via CI/CD** (no runtime secrets manager)
- **VictoriaMetrics** for metrics storage and querying
- **VictoriaLogs** for centralized logging (backend only needs structured logging)
- **Zipkin** for distributed tracing with Micrometer Tracing
- **Podman + systemd** for container orchestration
- **Self-hosted** infrastructure

### Integration Points

1. **VictoriaMetrics**: Scrapes `/actuator/prometheus` endpoint for metrics
2. **Zipkin**: Receives trace data via Micrometer Tracing integration
3. **VictoriaLogs**: Consumes structured JSON logs from application output
4. **Redis**: Used for caching and rate limiting (existing)

## Components and Interfaces

### Phase 1: Critical Security

#### 1.1 JWT Secret Validator

**Purpose**: Validate JWT secret strength at application startup to prevent weak secrets from being used in production.

**Component**: `JwtSecretValidator`
- Location: `br.dev.demoraes.abrolhos.application.config`
- Type: Spring Boot `@Component` with `@PostConstruct` initialization

**Interface**:
```kotlin
@Component
class JwtSecretValidator(
    @Value("\${jwt.secret}") private val jwtSecret: String
) {
    @PostConstruct
    fun validateJwtSecret()
}
```

**Behavior**:
- Executes during application startup (before accepting requests)
- Validates `jwt.secret` property is at least 32 characters
- Throws `IllegalStateException` with clear error message if validation fails
- Logs validation success at INFO level
- Application fails to start if validation fails (non-zero exit code)

**Dependencies**:
- Spring Boot property injection
- SLF4J logger

#### 1.2 Actuator Security Configuration

**Purpose**: Protect sensitive actuator endpoints with ADMIN role authorization while keeping health checks public.

**Component**: Update existing `SecurityConfig`
- Location: `br.dev.demoraes.abrolhos.infrastructure.web.config.SecurityConfig`
- Type: Spring Security configuration

**Changes**:
```kotlin
// Current: authorize("/actuator/**", permitAll)
// New:
authorize("/actuator/health", permitAll)
authorize("/actuator/health/**", permitAll)
authorize("/actuator/**", hasRole("ADMIN"))
```

**Behavior**:
- `/actuator/health` and `/actuator/health/**`: Public access (no authentication)
- All other `/actuator/**` endpoints: Require ADMIN role
- Unauthenticated requests: Return 401 Unauthorized
- Authenticated non-ADMIN requests: Return 403 Forbidden

**Dependencies**:
- Existing Spring Security configuration
- Existing JWT authentication filter
- Existing role-based authorization

#### 1.3 Generic Exception Handler

**Purpose**: Catch all unhandled exceptions and return consistent error responses with correlation IDs for traceability.

**Component**: Update existing `GlobalExceptionHandler`
- Location: `br.dev.demoraes.abrolhos.infrastructure.web.handlers.GlobalExceptionHandler`
- Type: `@RestControllerAdvice`

**New Handler**:
```kotlin
@ExceptionHandler(Exception::class)
fun handleGenericException(
    e: Exception,
    request: HttpServletRequest
): ResponseEntity<ErrorResponse>
```

**Behavior**:
- Catches all exceptions not handled by specific handlers
- Retrieves correlation ID from MDC
- Logs exception with correlation ID, exception type, message, and stack trace
- Returns HTTP 500 with error response including correlation ID
- Does not expose internal exception details to clients

**Error Response Format**:
```kotlin
data class ErrorResponse(
    val message: String,
    val status: Int,
    val correlationId: String? = null,
    val timestamp: Instant = Instant.now()
)
```

**Dependencies**:
- Existing `GlobalExceptionHandler`
- Correlation ID filter (Phase 2)
- SLF4J with MDC support

### Phase 2: Observability

#### 2.1 Correlation ID Filter

**Purpose**: Generate or extract correlation IDs for request tracing across logs and services.

**Component**: `CorrelationIdFilter`
- Location: `br.dev.demoraes.abrolhos.infrastructure.web.filters`
- Type: `OncePerRequestFilter`

**Interface**:
```kotlin
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    )
    
    companion object {
        const val CORRELATION_ID_HEADER = "X-Correlation-ID"
        const val CORRELATION_ID_MDC_KEY = "correlationId"
    }
}
```

**Behavior**:
1. Check for `X-Correlation-ID` header in incoming request
2. If present: Extract and use the provided correlation ID
3. If absent: Generate new UUID as correlation ID
4. Store correlation ID in MDC with key `correlationId`
5. Add correlation ID to response headers as `X-Correlation-ID`
6. Continue filter chain
7. Clear MDC after request completes (in finally block)

**Order**: Highest precedence to ensure correlation ID is available for all subsequent filters and handlers

**Dependencies**:
- SLF4J MDC
- Java UUID for generation

#### 2.2 Logback Configuration Update

**Purpose**: Include correlation ID in all log messages.

**Component**: Update existing `logback-spring.xml`
- Location: `src/main/resources/logback-spring.xml`

**Changes**:
- Add `%X{correlationId}` to log pattern
- Ensure JSON/ECS format includes correlation ID field
- Pattern example: `%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} [%X{correlationId}] - %msg%n`

**Behavior**:
- All log messages automatically include correlation ID when available
- Correlation ID appears as empty/null when not in request context
- Structured logging format includes correlation ID as separate field

#### 2.3 Micrometer Tracing Configuration

**Purpose**: Enable distributed tracing with Zipkin using Spring Boot 3.4 native Micrometer Tracing.

**Component**: `TracingConfig`
- Location: `br.dev.demoraes.abrolhos.application.config`
- Type: Spring configuration class

**Dependencies to Add** (build.gradle.kts):
```kotlin
implementation("io.micrometer:micrometer-tracing-bridge-brave")
implementation("io.zipkin.reporter2:zipkin-reporter-brave")
```

**Configuration** (application.yml):
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% sampling for single instance
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_ENDPOINT:http://localhost:9411/api/v2/spans}
```

**Interface**:
```kotlin
@Configuration
class TracingConfig {
    @Bean
    fun zipkinSpanHandler(
        @Value("\${management.zipkin.tracing.endpoint}") 
        zipkinEndpoint: String
    ): SpanHandler
}
```

**Behavior**:
- Automatically creates trace spans for all HTTP requests
- Generates trace ID and span ID for each request
- Sends trace data to Zipkin asynchronously
- Includes service name (`abrolhos`), operation name (HTTP method + path), timing
- Continues operating if Zipkin is unavailable (non-blocking)
- Integrates with correlation ID (trace ID can be used as correlation ID)

**Resilience**:
- Zipkin reporter uses async sender with queue
- Failed sends are logged but don't block requests
- Configurable retry and timeout settings

#### 2.4 Enhanced Metrics

**Purpose**: Add custom business metrics for monitoring application-specific behavior.

**Component**: Update existing `MetricsService`
- Location: `br.dev.demoraes.abrolhos.infrastructure.monitoring.MetricsService`

**New Metrics to Add**:

1. **Cache Metrics**:
```kotlin
private val cacheHits: Counter = Counter.builder("cache.hits")
    .description("Total number of cache hits")
    .tag("cache", "redis")
    .register(meterRegistry)

private val cacheMisses: Counter = Counter.builder("cache.misses")
    .description("Total number of cache misses")
    .tag("cache", "redis")
    .register(meterRegistry)
```

2. **Authentication Metrics** (enhance existing):
```kotlin
// Add tags to existing metrics
private fun recordAuthAttempt(success: Boolean, reason: String = "") {
    Counter.builder("auth.attempts")
        .tag("success", success.toString())
        .tag("reason", reason)
        .register(meterRegistry)
        .increment()
}
```

**Integration Points**:
- Cache: Add metrics recording to cache aspect or interceptor
- Authentication: Update `AuthService` to record attempts with tags
- Posts: Existing metrics already in place

**Exposure**:
- All metrics available at `/actuator/prometheus` in Prometheus format
- VictoriaMetrics scrapes this endpoint
- Metrics include common tags: `application=abrolhos`, `environment=${ENVIRONMENT}`

### Phase 3: Code Quality

#### 3.1 TODO Resolution

**Purpose**: Identify and resolve all TODO comments in the codebase.

**Process**:
1. Search codebase for TODO comments: `grep -r "TODO" src/`
2. For each TODO:
   - If represents incomplete functionality: Complete it
   - If represents future work: Document in issue tracker and remove comment
   - If represents technical debt: Create ticket and remove comment
3. Verify no TODOs remain before production deployment

**Deliverable**: Clean codebase with no TODO comments

#### 3.2 Debug Secret Logging Removal

**Purpose**: Audit and remove any logging of sensitive data.

**Process**:
1. Search for potential secret logging:
   - JWT tokens: `grep -r "jwt\|token" src/ | grep -i "log"`
   - Passwords: `grep -r "password" src/ | grep -i "log"`
   - API keys: `grep -r "api.*key\|secret" src/ | grep -i "log"`
2. Review each instance:
   - Remove logging of sensitive values
   - Replace with redacted versions if logging is necessary
   - Add tests to prevent regression

**Sensitive Data Redaction**:
```kotlin
object SensitiveDataRedactor {
    fun redactJwt(jwt: String): String = 
        if (jwt.length > 10) "${jwt.take(10)}..." else "***"
    
    fun redactPassword(password: String): String = "***"
    
    fun redactEmail(email: String): String = 
        email.replace(Regex("(?<=.{2}).(?=.*@)"), "*")
}
```

**Deliverable**: No sensitive data logged in any environment

#### 3.3 Security Hardening

**Purpose**: Implement security best practices for production deployment.

**3.3.1 Security Headers**

**Component**: `SecurityHeadersFilter`
- Location: `br.dev.demoraes.abrolhos.infrastructure.web.filters`
- Type: `OncePerRequestFilter`

**Headers to Add**:
```kotlin
response.setHeader("X-Content-Type-Options", "nosniff")
response.setHeader("X-Frame-Options", "DENY")
response.setHeader("X-XSS-Protection", "1; mode=block")
response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains")
response.setHeader("Content-Security-Policy", "default-src 'self'")
```

**Behavior**:
- Applied to all responses
- HSTS only in production environment
- Configurable via application properties

**3.3.2 HTTPS Redirect**

**Component**: Update `SecurityConfig`

**Configuration**:
```kotlin
@Profile("prod")
@Bean
fun httpsRedirectConfig(): SecurityFilterChain {
    http {
        requiresChannel {
            anyRequest { requiresSecure() }
        }
    }
}
```

**Behavior**:
- Only active in production profile
- Redirects all HTTP requests to HTTPS
- Returns 301 Moved Permanently

**3.3.3 CORS Validation**

**Component**: Review existing `CorsConfig`
- Location: `br.dev.demoraes.abrolhos.infrastructure.web.config.CorsConfig`

**Validation**:
- Verify `allowedOrigins` is explicitly configured (no wildcards)
- Ensure origins are validated against whitelist
- Confirm credentials are only allowed for trusted origins

**Current Implementation Review**:
```kotlin
// Verify this pattern is followed:
configuration.allowedOrigins = listOf(
    "https://app.example.com",
    "https://admin.example.com"
)
configuration.allowCredentials = true
configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE")
```

**3.3.4 Rate Limiting Verification**

**Component**: Review existing rate limiting configuration

**Verification**:
- Check `rate-limit.auth.max-attempts` is appropriate (default: 5)
- Check `rate-limit.auth.window-seconds` is appropriate (default: 300)
- Verify rate limiting is applied to authentication endpoints
- Confirm Redis-backed rate limiting is working

**No Changes Needed**: Existing implementation is sufficient

**3.3.5 Input Validation**

**Component**: Review existing validation

**Verification**:
- Confirm `@Valid` annotations on controller parameters
- Verify `@Validated` on service classes
- Check custom validators for business rules
- Ensure SQL injection prevention (JPA/Hibernate)
- Verify XSS prevention (JSON serialization)

**No Changes Needed**: Spring Boot validation and JPA provide sufficient protection

**3.3.6 Client IP Extraction Security**

**Component**: Review and update `RateLimitFilter` and `AuditAspect`

**Current Issue**:
- Both components extract client IP from `X-Forwarded-For` header directly
- Vulnerable to IP spoofing if not behind trusted reverse proxy
- Attacker can bypass rate limits or pollute audit logs

**Solution**:

1. **Configuration** (application.yml):
```yaml
server:
  forward-headers-strategy: framework
```

2. **Code Update**:
```kotlin
// Before (vulnerable):
val clientIp = request.getHeader("X-Forwarded-For")?.split(",")?.first()
    ?: request.remoteAddr

// After (secure):
val clientIp = request.remoteAddr
```

**Behavior**:
- Spring Boot's `ForwardedHeaderFilter` processes trusted proxy headers
- `request.remoteAddr` returns the actual client IP (not spoofable)
- Works correctly behind reverse proxies (Nginx, Apache, load balancers)
- Requires reverse proxy to set `X-Forwarded-For` header

**Files to Update**:
- `br.dev.demoraes.abrolhos.infrastructure.web.filters.RateLimitFilter`
- `br.dev.demoraes.abrolhos.application.audit.AuditAspect`

**Validation**:
- Verify rate limiting cannot be bypassed with spoofed headers
- Verify audit logs contain correct client IPs
- Test behind reverse proxy to ensure IPs are correct

## Data Models

### Correlation ID

- **Type**: String (UUID format)
- **Source**: HTTP header `X-Correlation-ID` or generated
- **Storage**: SLF4J MDC with key `correlationId`
- **Lifecycle**: Request scope (created at filter entry, cleared at filter exit)

### Error Response (Enhanced)

```kotlin
data class ErrorResponse(
    val message: String,
    val status: Int,
    val correlationId: String? = null,
    val timestamp: Instant = Instant.now()
)
```

### Trace Span

- **Trace ID**: Unique identifier for entire request flow
- **Span ID**: Unique identifier for specific operation
- **Service Name**: `abrolhos`
- **Operation Name**: HTTP method + path (e.g., `GET /api/posts`)
- **Tags**: HTTP status, method, path, error (if applicable)
- **Duration**: Request processing time in microseconds

### Metrics

All metrics follow Prometheus naming conventions:

- **Counters**: `{domain}.{action}.{unit}` (e.g., `auth.login.attempts`)
- **Timers**: `{domain}.{action}.time` (e.g., `posts.query.time`)
- **Tags**: Key-value pairs for filtering (e.g., `success=true`, `cache=redis`)

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Correlation ID Lifecycle

*For any* HTTP request, the application should generate a correlation ID if not provided, store it in MDC, include it in all log messages during request processing, include it in the response headers, and clear it from MDC after request completion.

**Validates: Requirements 4.1, 4.2, 4.3, 4.4, 4.5, 4.6**

### Property 2: Generic Exception Handling with Correlation ID

*For any* unhandled exception that occurs during request processing, the application should catch it, log it with the correlation ID from MDC, and return an HTTP 500 response with the correlation ID in the response body.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

### Property 3: Actuator Endpoint Authorization

*For any* actuator endpoint except `/actuator/health` and `/actuator/health/**`, the application should require ADMIN role authorization and return 401 for unauthenticated requests or 403 for authenticated non-ADMIN requests.

**Validates: Requirements 2.1, 2.2, 2.3**

### Property 4: Tracing Span Creation

*For any* HTTP request processed by the application, a trace span should be created with a trace ID, span ID, service name, operation name, and timing information, and the trace data should be sent to Zipkin.

**Validates: Requirements 5.2, 5.3, 5.4**

### Property 5: Post Creation Metrics

*For any* successful post creation operation, the application should increment the `posts.created` counter metric by exactly one.

**Validates: Requirements 6.1**

### Property 6: Authentication Metrics

*For any* authentication attempt, the application should increment the authentication attempt counter with appropriate success/failure tags indicating the outcome.

**Validates: Requirements 6.2**

### Property 7: Cache Metrics

*For any* cache operation (hit or miss), the application should increment the corresponding cache counter metric (`cache.hits` or `cache.misses`) with appropriate tags.

**Validates: Requirements 6.3**

### Property 8: Security Headers in Responses

*For any* HTTP response, the application should include security headers (X-Content-Type-Options, X-Frame-Options, X-XSS-Protection, and Strict-Transport-Security in production).

**Validates: Requirements 9.1**

### Property 9: HTTPS Redirect

*For any* HTTP request in production environment, the application should redirect to the HTTPS equivalent with a 301 status code.

**Validates: Requirements 9.2**

### Property 10: CORS Origin Validation

*For any* cross-origin request, the application should validate the origin against the configured whitelist and only allow requests from explicitly allowed origins.

**Validates: Requirements 9.3**

### Property 11: Input Validation and Sanitization

*For any* user input received through API endpoints, the application should validate the input against defined constraints and reject invalid input with appropriate error messages.

**Validates: Requirements 9.5**

### Property 12: Sensitive Data Redaction

*For any* logging operation that includes potentially sensitive data, the application should redact or mask sensitive fields (passwords, tokens, API keys) before writing to logs.

**Validates: Requirements 8.3**

### Property 13: Client IP Extraction Security

*For any* request where client IP is extracted for rate limiting or auditing, the application should use Spring Boot's forward headers strategy and `request.remoteAddr` to prevent IP spoofing attacks.

**Validates: Requirements 9.6**

## Error Handling

### JWT Secret Validation Errors

- **Scenario**: JWT secret is shorter than 32 characters
- **Handling**: Application fails to start with `IllegalStateException`
- **Logging**: ERROR level with clear message: "JWT secret must be at least 32 characters long. Current length: {length}"
- **Exit Code**: Non-zero (Spring Boot default behavior)

### Actuator Authorization Errors

- **Scenario**: Unauthenticated request to protected actuator endpoint
- **Handling**: Return 401 Unauthorized
- **Logging**: WARN level with endpoint and correlation ID
- **Response**: Standard Spring Security error response

- **Scenario**: Authenticated non-ADMIN request to protected actuator endpoint
- **Handling**: Return 403 Forbidden
- **Logging**: WARN level with username, endpoint, and correlation ID
- **Response**: Standard Spring Security error response

### Generic Exception Handling

- **Scenario**: Any unhandled exception during request processing
- **Handling**: Catch in `GlobalExceptionHandler`
- **Logging**: ERROR level with correlation ID, exception type, message, and stack trace
- **Response**: HTTP 500 with `ErrorResponse` including correlation ID
- **Client Message**: Generic "Internal server error" (no internal details exposed)

### Tracing Errors

- **Scenario**: Zipkin is unavailable
- **Handling**: Log warning and continue operation
- **Logging**: WARN level: "Failed to send trace data to Zipkin"
- **Impact**: No impact on request processing (non-blocking)

### Metrics Recording Errors

- **Scenario**: Metrics recording fails
- **Handling**: Log error and continue operation
- **Logging**: ERROR level with metric name and error details
- **Impact**: No impact on request processing (non-blocking)

### Correlation ID Errors

- **Scenario**: Invalid correlation ID format in header
- **Handling**: Accept as-is (no validation) or generate new UUID
- **Logging**: DEBUG level if generating new ID
- **Impact**: No impact on request processing

## Testing Strategy

### Dual Testing Approach

The testing strategy employs both unit tests and property-based tests as complementary approaches:

- **Unit Tests**: Verify specific examples, edge cases, and error conditions
- **Property Tests**: Verify universal properties across all inputs using randomization
- **Balance**: Avoid excessive unit tests; property tests handle comprehensive input coverage

### Property-Based Testing Configuration

- **Library**: Kotest Property Testing (already in dependencies)
- **Iterations**: Minimum 100 iterations per property test
- **Tagging**: Each property test must reference its design document property
- **Tag Format**: `// Feature: production-readiness-improvements, Property {number}: {property_text}`

### Test Coverage by Phase

#### Phase 1: Critical Security

**JWT Secret Validation**:
- **Example Test**: Application startup with 31-character secret (should fail)
- **Example Test**: Application startup with 32-character secret (should succeed)
- **Example Test**: Application startup with 64-character secret (should succeed)
- **Edge Case**: Empty secret
- **Edge Case**: Null secret

**Actuator Security**:
- **Property Test**: All actuator endpoints except health require ADMIN role
- **Example Test**: `/actuator/health` is publicly accessible
- **Edge Case**: Unauthenticated request returns 401
- **Edge Case**: Non-ADMIN authenticated request returns 403

**Generic Exception Handler**:
- **Property Test**: All unhandled exceptions are caught and logged with correlation ID
- **Example Test**: Specific exception type returns 500 with correlation ID
- **Edge Case**: Exception with null message
- **Edge Case**: Exception during exception handling

#### Phase 2: Observability

**Correlation ID**:
- **Property Test**: Correlation ID lifecycle (generation, MDC storage, logging, response headers, cleanup)
- **Example Test**: Request with provided correlation ID uses that ID
- **Example Test**: Request without correlation ID generates new UUID
- **Edge Case**: Invalid UUID format in header
- **Edge Case**: Very long correlation ID string

**Micrometer Tracing**:
- **Property Test**: All requests generate trace spans with required fields
- **Example Test**: Zipkin configuration is correct at startup
- **Edge Case**: Zipkin unavailable (application continues)
- **Integration Test**: Trace data appears in Zipkin

**Enhanced Metrics**:
- **Property Test**: Post creation increments counter
- **Property Test**: Authentication attempts increment counter with tags
- **Property Test**: Cache operations increment appropriate counters
- **Example Test**: Metrics endpoint returns Prometheus format
- **Integration Test**: VictoriaMetrics can scrape metrics

#### Phase 3: Code Quality

**TODO Resolution**:
- **Manual Review**: Search codebase for TODO comments
- **Verification**: No TODOs in production branch

**Debug Secret Logging**:
- **Manual Review**: Audit logging statements for sensitive data
- **Property Test**: Redaction functions properly mask sensitive data
- **Unit Test**: Specific redaction examples (JWT, password, email)

**Security Hardening**:
- **Property Test**: All responses include security headers
- **Property Test**: HTTP requests redirect to HTTPS in production
- **Property Test**: CORS validates origins against whitelist
- **Property Test**: Input validation rejects invalid data
- **Property Test**: Client IP extraction uses secure method (not spoofable)
- **Example Test**: Specific security header values
- **Example Test**: CORS preflight request handling
- **Example Test**: Rate limiting with spoofed X-Forwarded-For header (should not bypass)
- **Integration Test**: End-to-end HTTPS redirect

### Test Organization

```
src/test/kotlin/br/dev/demoraes/abrolhos/
├── application/
│   └── config/
│       └── JwtSecretValidatorTest.kt
├── infrastructure/
│   ├── web/
│   │   ├── config/
│   │   │   └── SecurityConfigTest.kt
│   │   ├── filters/
│   │   │   ├── CorrelationIdFilterTest.kt
│   │   │   ├── SecurityHeadersFilterTest.kt
│   │   │   └── RateLimitFilterTest.kt
│   │   └── handlers/
│   │       └── GlobalExceptionHandlerTest.kt
│   └── monitoring/
│       └── MetricsServiceTest.kt
└── properties/
    ├── CorrelationIdPropertiesTest.kt
    ├── ExceptionHandlingPropertiesTest.kt
    ├── ActuatorSecurityPropertiesTest.kt
    ├── TracingPropertiesTest.kt
    ├── MetricsPropertiesTest.kt
    ├── SecurityHeadersPropertiesTest.kt
    ├── HttpsRedirectPropertiesTest.kt
    ├── CorsValidationPropertiesTest.kt
    ├── InputValidationPropertiesTest.kt
    ├── SensitiveDataRedactionPropertiesTest.kt
    └── ClientIpSecurityPropertiesTest.kt
```

### Property Test Example

```kotlin
class CorrelationIdPropertiesTest : FunSpec({
    // Feature: production-readiness-improvements, Property 1: Correlation ID Lifecycle
    test("For any HTTP request, correlation ID should be generated, stored, logged, returned, and cleaned up") {
        checkAll(100, Arb.string(), Arb.string()) { path, body ->
            // Test implementation
        }
    }
})
```

### Integration Testing

- **Testcontainers**: Use for Redis, PostgreSQL, Zipkin
- **Spring Boot Test**: Use `@SpringBootTest` for full context
- **MockMvc**: Use for HTTP request/response testing
- **WireMock**: Use for external service mocking (if needed)

### Manual Testing Checklist

- [ ] JWT secret validation prevents startup with weak secret
- [ ] Actuator endpoints require ADMIN role (except health)
- [ ] Correlation IDs appear in logs and responses
- [ ] Trace data appears in Zipkin UI
- [ ] Metrics appear in VictoriaMetrics
- [ ] Security headers present in all responses
- [ ] HTTPS redirect works in production
- [ ] CORS blocks unauthorized origins
- [ ] No TODO comments in codebase
- [ ] No sensitive data in logs

## Implementation Notes

### Phase Ordering

The three phases should be implemented in order:

1. **Phase 1 (Critical Security)**: Must be completed first as it addresses security vulnerabilities
2. **Phase 2 (Observability)**: Depends on Phase 1 (correlation ID used in exception handler)
3. **Phase 3 (Code Quality)**: Can be done in parallel with Phase 2, but security hardening should be last

### Configuration Management

All configuration should be externalized via environment variables:

- `JWT_SECRET`: JWT signing secret (minimum 32 characters)
- `ZIPKIN_ENDPOINT`: Zipkin server URL (default: http://localhost:9411/api/v2/spans)
- `ENVIRONMENT`: Environment name for metrics tags (default: dev)
- `SECURITY_CORS_ALLOWED_ORIGINS`: Comma-separated list of allowed CORS origins

### Backward Compatibility

- Existing endpoints and behavior remain unchanged
- New correlation ID header is optional (generated if not provided)
- Actuator security change may break existing monitoring (document migration)
- All changes are additive except actuator security

### Performance Considerations

- Correlation ID filter has minimal overhead (UUID generation + MDC operations)
- Tracing has minimal overhead with async reporter
- Metrics recording is non-blocking
- Security headers filter has negligible overhead

### Monitoring the Monitors

- Monitor Zipkin connectivity (log warnings if unavailable)
- Monitor metrics recording failures (log errors)
- Monitor filter execution time (add metrics if needed)
- Alert on high exception rates (indicates problems)

### Rollback Plan

If issues arise after deployment:

1. **JWT Secret Validation**: Can be disabled via feature flag if needed
2. **Actuator Security**: Can be reverted to `permitAll` temporarily
3. **Correlation ID**: Can be disabled by removing filter from chain
4. **Tracing**: Can be disabled via `management.tracing.enabled=false`
5. **Metrics**: Can be disabled individually if causing issues

### Documentation Updates

- Update README with new environment variables
- Document correlation ID header for API consumers
- Document actuator security changes for ops team
- Update deployment guide with Zipkin configuration
- Add troubleshooting guide for observability features
