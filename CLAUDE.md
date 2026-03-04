# CLAUDE.md — abrolhos (backend)

Personal blog API. Kotlin/Spring Boot 3 REST service.

## Commands

```bash
./gradlew bootRun          # Run the application
./gradlew test             # Run all tests
./gradlew test --tests "*.ClassName"  # Run a specific test class
./gradlew detekt           # Lint (Detekt)
./gradlew generateOpenApiDocs  # Generate OpenAPI spec to build/
```

## Stack

- **Language**: Kotlin 1.9 / JVM 21
- **Framework**: Spring Boot 3.4.8 (Web, Security, Data JPA, Cache, Actuator, Validation)
- **Database**: PostgreSQL (Testcontainers in tests)
- **Migrations**: Flyway
- **Cache**: Redis (Spring Cache + `GenericJackson2JsonRedisSerializer` with default typing)
- **Auth**: JWT (auth0 `java-jwt`) + TOTP (`kotlin-onetimepassword`) + Password
- **Rate limiting**: Bucket4j + Redis
- **Monitoring**: Micrometer + Prometheus + Zipkin (Brave)
- **API docs**: SpringDoc OpenAPI (`/swagger-ui.html`, `/v3/api-docs`)
- **Testing**: JUnit 5, MockK, springmockk, Kotest (assertions + property), Testcontainers
- **Lint**: Detekt + detekt-formatting

## Project Structure

```
src/main/kotlin/br/dev/demoraes/abrolhos/
  domain/
    entities/          # Domain interfaces and data classes (Post, User, PostSummary…)
    repository/        # Repository interfaces (domain contracts)
    exceptions/        # Domain-specific exception hierarchies
  application/
    services/          # Business logic (PostService, AuthService, TokenService…)
    config/            # App-level config beans (PasswordConfiguration, JwtSecretValidator)
    audit/             # AOP-based audit logging
    utils/             # SensitiveDataRedactor
  infrastructure/
    persistence/
      entities/        # JPA @Entity classes (PostEntity, UserEntity…)
      postgresql/      # Spring Data JPA repositories (interfaces extending JpaRepository)
      *RepositoryImpl  # Adapters implementing domain repository interfaces
      converters/      # JPA AttributeConverters (TotpSecretConverter)
    web/
      controllers/     # @RestController classes
      dto/
        request/       # Incoming request bodies
        response/      # Outgoing response bodies
      filters/         # OncePerRequestFilter (JWT, CORS, rate limit, correlation ID)
      handlers/        # GlobalExceptionHandler
      config/          # SecurityConfig, CorsConfig, WebConfig…
    cache/
      CacheConfig.kt   # RedisCacheManager + CachingConfigurer (fail-open error handler)
      config/          # RedisConfig (connection + RedisTemplate)
      dto/             # Serializable cache DTOs (PostSummaryDto, SerializablePage)
    monitoring/
      MetricsService.kt
      health/          # Custom HealthIndicator implementations
```

Migrations live in `src/main/resources/db/migration/` (Flyway `V__*.sql` naming).

## Layering Rules

- **Domain** has zero infrastructure imports. Entities and repository interfaces are pure Kotlin.
- **Application services** depend on domain interfaces only — never on JPA or Spring Data directly.
- **Infrastructure** implements domain interfaces and owns all framework-specific code.
- **Controllers** are thin: validate input, call a service, map to response DTO.
- **No business logic in controllers or filters.**

## Naming Conventions

| Thing | Convention | Example |
|---|---|---|
| Domain entity / interface | PascalCase | `Post`, `PostSummary` |
| JPA entity | `*Entity` suffix | `PostEntity`, `UserEntity` |
| Spring Data repository | `*Postgresql` suffix | `PostRepositoryPostgresql` |
| Domain repository impl | `*RepositoryImpl` suffix | `PostRepositoryImpl` |
| Request DTOs | `*Request` suffix | `CreatePostRequest` |
| Response DTOs | `*Response` suffix | `PostSummaryResponse` |
| Services | `*Service` suffix | `PostService` |
| Filters | `*Filter` suffix | `JwtAuthenticationFilter` |

## Testing Conventions

- **Unit tests** for services: MockK (`mockk<T>()`, `every { }`, `verify { }`), springmockk for Spring beans.
- **Integration tests** for repositories/controllers: `@SpringBootTest` or `@WebMvcTest` + Testcontainers (PostgreSQL).
- **Property tests**: Kotest Property (`forAll`, `Arb`) in `*PropertyTest` classes.
- Test files live in `src/test/kotlin/` mirroring the main source tree.
- Name tests as sentences: `"should return 404 when post slug does not exist"`.
- Use `@Nested` inner classes to group related scenarios within a test class.

## Cache

- `postSummaries` and `postBySlug` caches, 7-day TTL.
- Values serialized with `GenericJackson2JsonRedisSerializer` + `activateDefaultTyping` (adds `@class` field).
- Error handler is fail-open: `CacheConfig` implements `CachingConfigurer` and overrides `errorHandler()` to log GET/PUT/EVICT errors as WARN and swallow them — the app falls back to the DB.
- Cache DTOs (`PostSummaryDto`, `SerializablePage`) must be concrete, Jackson-serializable classes — JPA proxies are not cacheable.
- `searchSummary` JPQL query orders by `publishedAt DESC, id DESC` — paginated post lists return newest-first, consistent with the cursor-based endpoint.

## Security

- JWT issued on login, validated in `JwtAuthenticationFilter`.
- Password-based auth available alongside TOTP.
- Rate limiting via Bucket4j + Redis (`RateLimitFilter`).
- Correlation IDs injected by `CorrelationIdFilter` and included in all log lines.

## Known Inconsistencies / Technical Debt

- Zipkin traces drop silently when no Zipkin server is running locally (expected in dev).
