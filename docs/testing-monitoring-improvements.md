# Testing & Monitoring Improvements for Abrolhos

## Overview
This document outlines comprehensive testing and monitoring improvements for the Abrolhos application to ensure reliability, observability, and maintainability.

## Current Testing & Monitoring Gaps

### 1. Limited Integration Test Coverage
**Location**: Backend test suite

**Issue**: Strong property-based tests for TOTP, but limited integration tests for API endpoints and business flows.

**Impact**:
- Bugs slip through to production
- Regression risks on refactoring
- Difficult to validate end-to-end flows
- No contract testing between frontend/backend

### 2. No Monitoring or Observability
**Location**: Production environment

**Issue**: No metrics collection, distributed tracing, or application performance monitoring.

**Impact**:
- Cannot detect performance degradation
- Difficult to diagnose production issues
- No visibility into user experience
- Reactive rather than proactive support

### 3. Missing E2E Tests
**Location**: Frontend test suite

**Issue**: No end-to-end tests validating complete user journeys.

**Impact**:
- Integration bugs between frontend/backend
- UI regressions not caught
- User flows not validated
- Manual testing required for releases

### 4. No Performance Testing
**Location**: Test suite

**Issue**: No load testing, stress testing, or performance benchmarking.

**Impact**:
- Unknown system capacity
- Performance regressions not detected
- Scalability issues discovered in production
- No SLA validation

### 5. Limited Health Checks
**Location**: Spring Boot Actuator

**Issue**: Basic health checks exist but not comprehensive (database, Redis, external dependencies).

**Impact**:
- Cannot detect partial outages
- Poor monitoring integration
- Difficult to implement circuit breakers
- No readiness/liveness distinction

---

## Proposed Solutions

### Solution 1: Expand Integration Test Coverage

#### Requirements
```xml
<requirement id="TEST-001" priority="high">
  <title>Comprehensive Integration Tests</title>
  <description>
    Add integration tests for all API endpoints and critical business flows
  </description>
  <acceptance-criteria>
    - Integration tests for all REST endpoints
    - Tests use real database (Testcontainers)
    - Tests validate request/response contracts
    - Tests cover authentication flows
    - Tests cover authorization scenarios
    - Tests validate error responses
    - Integration test coverage &gt; 80%
  </acceptance-criteria>
</requirement>
```

#### Implementation Details

**Testcontainers Setup**:
```kotlin
// src/test/kotlin/br/dev/demoraes/abrolhos/IntegrationTestBase.kt
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
abstract class IntegrationTestBase {
    
    companion object {
        @Container
        val postgres = PostgreSQLContainer<Nothing>("postgres:16-alpine").apply {
            withDatabaseName("abrolhos_test")
            withUsername("test")
            withPassword("test")
        }
        
        @Container
        val redis = GenericContainer<Nothing>("redis:7-alpine").apply {
            withExposedPorts(6379)
        }
        
        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.redis.host", redis::getHost)
            registry.add("spring.redis.port", redis::getFirstMappedPort)
        }
    }
    
    @Autowired
    protected lateinit var restTemplate: TestRestTemplate
    
    @Autowired
    protected lateinit var objectMapper: ObjectMapper
}
```

**API Integration Tests**:
```kotlin
// src/test/kotlin/br/dev/demoraes/abrolhos/api/PostApiIntegrationTest.kt
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostApiIntegrationTest : IntegrationTestBase() {
    
    @Test
    fun `should return published posts`() {
        // Given
        val response = restTemplate.getForEntity(
            "/api/posts?status=PUBLISHED&page=0&size=10",
            PostSummaryPageResponse::class.java
        )
        
        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.content).isNotEmpty()
    }
    
    @Test
    fun `should return post by slug`() {
        // Given
        val slug = "test-post"
        
        // When
        val response = restTemplate.getForEntity(
            "/api/posts/$slug",
            PostResponse::class.java
        )
        
        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.slug).isEqualTo(slug)
    }
    
    @Test
    fun `should require authentication for post creation`() {
        // Given
        val request = CreatePostRequest(
            title = "New Post",
            slug = "new-post",
            content = "Content",
            status = PostStatus.DRAFT,
            categoryIds = listOf(UUID.randomUUID()),
            tagIds = emptyList()
        )
        
        // When
        val response = restTemplate.postForEntity(
            "/api/posts",
            request,
            ErrorResponse::class.java
        )
        
        // Then
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
```

#### Testing Strategy
- Test all happy paths
- Test error scenarios (400, 401, 403, 404, 500)
- Test pagination and filtering
- Test data validation
- Test concurrent requests

---

### Solution 2: Implement Application Monitoring

#### Requirements
```xml
<requirement id="MON-001" priority="critical">
  <title>Application Performance Monitoring</title>
  <description>
    Implement comprehensive monitoring with metrics, tracing, and logging
  </description>
  <acceptance-criteria>
    - Prometheus metrics exposed via actuator
    - Custom business metrics tracked
    - Distributed tracing with OpenTelemetry
    - Structured JSON logging
    - Log aggregation configured
    - Grafana dashboards for key metrics
    - Alerting rules configured
    - SLO/SLI tracking implemented
  </acceptance-criteria>
</requirement>
```

#### Implementation Details

**Prometheus Metrics**:
```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
}

// application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: abrolhos
      environment: ${ENVIRONMENT:dev}
```

**Custom Metrics**:
```kotlin
// src/main/kotlin/br/dev/demoraes/abrolhos/application/config/MetricsConfig.kt
@Configuration
class MetricsConfig {
    
    @Bean
    fun meterRegistryCustomizer(): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry ->
            registry.config().commonTags(
                "application", "abrolhos",
                "version", "1.0.0"
            )
        }
    }
}

// src/main/kotlin/br/dev/demoraes/abrolhos/application/services/MetricsService.kt
@Service
class MetricsService(private val meterRegistry: MeterRegistry) {
    
    private val loginAttempts = meterRegistry.counter("auth.login.attempts")
    private val loginSuccesses = meterRegistry.counter("auth.login.success")
    private val loginFailures = meterRegistry.counter("auth.login.failure")
    private val postCreations = meterRegistry.counter("posts.created")
    private val postViews = meterRegistry.counter("posts.views")
    
    fun recordLoginAttempt() = loginAttempts.increment()
    fun recordLoginSuccess() = loginSuccesses.increment()
    fun recordLoginFailure() = loginFailures.increment()
    fun recordPostCreation() = postCreations.increment()
    fun recordPostView() = postViews.increment()
    
    fun recordPostQueryTime(duration: Duration) {
        meterRegistry.timer("posts.query.time").record(duration)
    }
}
```

**Structured Logging**:
```xml
<!-- src/main/resources/logback-spring.xml -->
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"application":"abrolhos"}</customFields>
        </encoder>
    </appender>
    
    <root level="INFO">
        <appender-ref ref="JSON" />
    </root>
</configuration>
```

**Key Metrics to Track**:
- Request rate (requests/second)
- Response time (p50, p95, p99)
- Error rate (%)
- Login success/failure rate
- Post creation rate
- Post view count
- Database query time
- Cache hit rate
- JVM metrics (heap, GC, threads)

**Grafana Dashboard Panels**:
1. Request Rate & Response Time
2. Error Rate & Status Codes
3. Authentication Metrics
4. Post Metrics
5. Database Performance
6. Cache Performance
7. JVM Health
8. System Resources

---

### Solution 3: Add End-to-End Tests

#### Requirements
```xml
<requirement id="TEST-002" priority="high">
  <title>End-to-End Test Suite</title>
  <description>
    Implement E2E tests using Playwright to validate complete user journeys
  </description>
  <acceptance-criteria>
    - E2E tests for user login flow
    - E2E tests for post creation flow
    - E2E tests for post viewing flow
    - E2E tests for navigation
    - Tests run in CI/CD pipeline
    - Tests run against staging environment
    - Visual regression testing included
    - Mobile viewport testing included
  </acceptance-criteria>
</requirement>
```

#### Implementation Details

**Playwright Setup**:
```typescript
// Frontend/abrolhos-frontend/playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:5173',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'mobile',
      use: { ...devices['iPhone 13'] },
    },
  ],
  webServer: {
    command: 'bun run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
  },
});
```

**E2E Test Examples**:
```typescript
// e2e/auth.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Authentication', () => {
  test('should login successfully with valid credentials', async ({ page }) => {
    await page.goto('/');
    
    // Click login button
    await page.click('text=Login');
    
    // Fill login form
    await page.fill('input[name="username"]', 'testuser');
    await page.fill('input[name="totpCode"]', '123456');
    
    // Submit form
    await page.click('button[type="submit"]');
    
    // Verify redirect to dashboard
    await expect(page).toHaveURL('/posts');
    await expect(page.locator('text=Welcome')).toBeVisible();
  });
  
  test('should show error with invalid credentials', async ({ page }) => {
    await page.goto('/');
    await page.click('text=Login');
    
    await page.fill('input[name="username"]', 'invalid');
    await page.fill('input[name="totpCode"]', '000000');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('text=Invalid credentials')).toBeVisible();
  });
});
```

```typescript
// e2e/posts.spec.ts
test.describe('Post Management', () => {
  test.beforeEach(async ({ page }) => {
    // Login before each test
    await page.goto('/');
    await page.click('text=Login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="totpCode"]', '123456');
    await page.click('button[type="submit"]');
  });
  
  test('should create new post', async ({ page }) => {
    await page.goto('/posts/create');
    
    await page.fill('input[name="title"]', 'Test Post');
    await page.fill('input[name="slug"]', 'test-post');
    await page.fill('textarea[name="content"]', 'Test content');
    await page.selectOption('select[name="status"]', 'DRAFT');
    
    await page.click('button[type="submit"]');
    
    await expect(page.locator('text=Post created successfully')).toBeVisible();
  });
  
  test('should view post details', async ({ page }) => {
    await page.goto('/posts/test-post');
    
    await expect(page.locator('h1')).toContainText('Test Post');
    await expect(page.locator('article')).toBeVisible();
  });
});
```

---

### Solution 4: Implement Performance Testing

#### Requirements
```xml
<requirement id="TEST-003" priority="medium">
  <title>Performance and Load Testing</title>
  <description>
    Implement load testing to validate system performance and capacity
  </description>
  <acceptance-criteria>
    - Load tests for critical endpoints
    - Stress tests to find breaking points
    - Soak tests for stability validation
    - Performance benchmarks established
    - Tests run in CI/CD pipeline
    - Performance regression detection
    - Capacity planning data collected
  </acceptance-criteria>
</requirement>
```

#### Implementation Details

**K6 Load Testing**:
```javascript
// load-tests/posts-list.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '1m', target: 50 },   // Ramp up to 50 users
    { duration: '3m', target: 50 },   // Stay at 50 users
    { duration: '1m', target: 100 },  // Ramp up to 100 users
    { duration: '3m', target: 100 },  // Stay at 100 users
    { duration: '1m', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests under 500ms
    errors: ['rate<0.05'],             // Error rate under 5%
  },
};

export default function () {
  const res = http.get('http://localhost:8080/api/posts?status=PUBLISHED&page=0&size=10');
  
  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 500ms': (r) => r.timings.duration < 500,
  });
  
  errorRate.add(!success);
  sleep(1);
}
```

**Gatling Load Testing** (alternative):
```scala
// load-tests/PostsSimulation.scala
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class PostsSimulation extends Simulation {
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
  
  val scn = scenario("Posts Load Test")
    .exec(http("Get Posts")
      .get("/api/posts?status=PUBLISHED&page=0&size=10")
      .check(status.is(200))
      .check(responseTimeInMillis.lte(500)))
    .pause(1)
  
  setUp(
    scn.inject(
      rampUsers(100) during (2.minutes)
    )
  ).protocols(httpProtocol)
   .assertions(
     global.responseTime.percentile3.lt(500),
     global.successfulRequests.percent.gt(95)
   )
}
```

---

### Solution 5: Enhanced Health Checks

#### Requirements
```xml
<requirement id="MON-002" priority="high">
  <title>Comprehensive Health Checks</title>
  <description>
    Implement detailed health checks for all system dependencies
  </description>
  <acceptance-criteria>
    - Database health check with connection pool status
    - Redis health check with latency measurement
    - Disk space health check
    - Custom business health indicators
    - Readiness vs liveness distinction
    - Health check response time &lt; 100ms
    - Graceful degradation on partial failures
  </acceptance-criteria>
</requirement>
```

#### Implementation Details

**Custom Health Indicators**:
```kotlin
// src/main/kotlin/br/dev/demoraes/abrolhos/application/health/DatabaseHealthIndicator.kt
@Component
class DatabaseHealthIndicator(
    private val dataSource: DataSource
) : HealthIndicator {
    
    override fun health(): Health {
        return try {
            dataSource.connection.use { conn ->
                val start = System.currentTimeMillis()
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT 1").use { rs ->
                        rs.next()
                    }
                }
                val duration = System.currentTimeMillis() - start
                
                Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("responseTime", "${duration}ms")
                    .build()
            }
        } catch (e: Exception) {
            Health.down()
                .withDetail("error", e.message)
                .build()
        }
    }
}
```

```kotlin
// Redis Health Indicator
@Component
class RedisHealthIndicator(
    private val redisTemplate: RedisTemplate<String, String>
) : HealthIndicator {
    
    override fun health(): Health {
        return try {
            val start = System.currentTimeMillis()
            redisTemplate.opsForValue().set("health:check", "ok", Duration.ofSeconds(1))
            val value = redisTemplate.opsForValue().get("health:check")
            val duration = System.currentTimeMillis() - start
            
            if (value == "ok") {
                Health.up()
                    .withDetail("redis", "available")
                    .withDetail("latency", "${duration}ms")
                    .build()
            } else {
                Health.down()
                    .withDetail("error", "Unexpected response")
                    .build()
            }
        } catch (e: Exception) {
            Health.down()
                .withDetail("error", e.message)
                .build()
        }
    }
}

// Disk Space Health Indicator
@Component
class DiskSpaceHealthIndicator : HealthIndicator {
    
    private val threshold = 1024 * 1024 * 1024 // 1GB
    
    override fun health(): Health {
        val file = File(".")
        val freeSpace = file.freeSpace
        val totalSpace = file.totalSpace
        val usedSpace = totalSpace - freeSpace
        val usedPercent = (usedSpace.toDouble() / totalSpace * 100).toInt()
        
        return if (freeSpace > threshold) {
            Health.up()
                .withDetail("free", "${freeSpace / 1024 / 1024}MB")
                .withDetail("total", "${totalSpace / 1024 / 1024}MB")
                .withDetail("used", "$usedPercent%")
                .build()
        } else {
            Health.down()
                .withDetail("error", "Low disk space")
                .withDetail("free", "${freeSpace / 1024 / 1024}MB")
                .build()
        }
    }
}
```

**Readiness vs Liveness**:
```yaml
# application.yml
management:
  endpoint:
    health:
      probes:
        enabled: true
      group:
        readiness:
          include: readinessState,db,redis
        liveness:
          include: livenessState,diskSpace
```

**Kubernetes Probes**:
```yaml
# k8s/deployment.yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
```

---

## Testing Strategy Summary

### Unit Tests
- Service layer logic
- Repository implementations
- Utility functions
- Value object validation
- Property-based tests for algorithms

### Integration Tests
- API endpoints with real database
- Authentication flows
- Authorization scenarios
- Database queries
- Cache behavior

### E2E Tests
- Complete user journeys
- Cross-browser compatibility
- Mobile responsiveness
- Visual regression

### Performance Tests
- Load testing (normal traffic)
- Stress testing (peak traffic)
- Soak testing (sustained load)
- Spike testing (sudden traffic)

---

## Monitoring Strategy Summary

### Application Metrics
- Request rate and response time
- Error rate by endpoint
- Business metrics (logins, posts)
- Cache hit rate
- Database query performance

### Infrastructure Metrics
- CPU and memory usage
- Disk I/O and space
- Network throughput
- Container health

### Logs
- Structured JSON logs
- Log levels (ERROR, WARN, INFO, DEBUG)
- Correlation IDs for tracing
- Sensitive data redaction

### Alerts
- High error rate (> 5%)
- Slow response time (p95 > 1s)
- High CPU usage (> 80%)
- Low disk space (< 1GB)
- Database connection pool exhaustion

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1)
1. Set up Testcontainers (1 day)
2. Add basic integration tests (2 days)
3. Configure Prometheus metrics (1 day)
4. Set up structured logging (1 day)

### Phase 2: Monitoring (Week 2)
1. Implement custom metrics (2 days)
2. Create Grafana dashboards (2 days)
3. Configure alerting rules (1 day)

### Phase 3: E2E Testing (Week 3)
1. Set up Playwright (1 day)
2. Write E2E tests for critical flows (3 days)
3. Integrate with CI/CD (1 day)

### Phase 4: Performance (Week 4)
1. Set up K6 or Gatling (1 day)
2. Write load test scenarios (2 days)
3. Run baseline performance tests (1 day)
4. Document performance benchmarks (1 day)

---

## Success Metrics

### Test Coverage
- Unit test coverage > 80%
- Integration test coverage > 70%
- E2E test coverage for all critical flows
- Property-based tests for core algorithms

### Monitoring Coverage
- All critical endpoints monitored
- All dependencies health checked
- All errors logged and tracked
- SLO compliance > 99%

### Performance Targets
- API response time p95 < 500ms
- System handles 1000 concurrent users
- Error rate < 1%
- Uptime > 99.9%

---

## Dependencies

### Backend Testing
```kotlin
// Testcontainers
testImplementation("org.testcontainers:testcontainers:1.19.3")
testImplementation("org.testcontainers:postgresql:1.19.3")
testImplementation("org.testcontainers:junit-jupiter:1.19.3")

// Monitoring
implementation("io.micrometer:micrometer-registry-prometheus")
implementation("io.micrometer:micrometer-tracing-bridge-otel")
implementation("net.logstash.logback:logstash-logback-encoder:7.4")
```

### Frontend Testing
```json
{
  "devDependencies": {
    "@playwright/test": "^1.40.1",
    "playwright": "^1.40.1"
  }
}
```

### Load Testing
- K6 (https://k6.io/)
- Gatling (https://gatling.io/)

---

## Rollback Plan

### If Tests Fail in CI/CD
1. Identify failing tests
2. Determine if test or code issue
3. Fix or skip flaky tests temporarily
4. Document known issues

### If Monitoring Overhead Too High
1. Reduce metric collection frequency
2. Disable non-critical metrics
3. Optimize metric queries
4. Scale monitoring infrastructure

---

## Documentation Requirements

1. **Testing Guide**
   - How to run tests locally
   - How to write new tests
   - Testing best practices
   - CI/CD integration

2. **Monitoring Guide**
   - Available metrics and dashboards
   - How to create alerts
   - How to investigate issues
   - Runbook for common problems

3. **Performance Benchmarks**
   - Baseline performance metrics
   - Capacity planning data
   - Load test results
   - Optimization recommendations
