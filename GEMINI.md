# GEMINI.md — Abrolhos (Backend)

Kotlin/Spring Boot 3 REST service for the Abrolhos blog.

## Core Commands

- `./gradlew bootRun` — Run the application.
- `./gradlew test` — Run all tests.
- `./gradlew test --tests "*.ClassName"` — Run a specific test class.
- `./gradlew detekt` — Run linter (Detekt).
- `./gradlew generateOpenApiDocs` — Generate OpenAPI spec (to `build/`).

## Tech Stack

- **Kotlin 1.9 / JVM 21**
- **Spring Boot 3.4.8**
- **PostgreSQL / Flyway**
- **Redis (Cache + Rate Limiting)**
- **JWT + TOTP Auth**
- **JUnit 5 / MockK / Kotest**
- **Detekt**

## Architectural Layering

Follow strict layering rules:
- **Domain**: Pure Kotlin. No framework imports. Entities, repository interfaces, exceptions.
- **Application Services**: Business logic. Depend on domain interfaces only.
- **Infrastructure**: Implementation of domain contracts. JPA entities, repository adapters, filters, config.
- **Web**: Controllers (thin), DTOs (request/response).

## Naming Conventions

- **Domain Entity**: `Post`, `User`
- **JPA Entity**: `PostEntity`
- **JPA Repository**: `PostRepositoryPostgresql` (interface)
- **Repository Implementation**: `PostRepositoryImpl` (adapter)
- **DTOs**: `*Request`, `*Response`
- **Services**: `*Service`

## Testing Standards

- **Unit tests**: MockK for services.
- **Integration tests**: `@SpringBootTest` or `@WebMvcTest` with Testcontainers (PostgreSQL).
- **Property tests**: Kotest Property (`forAll`, `Arb`) in `*PropertyTest` classes.
- Name tests as sentences: `"should return 404 when post slug does not exist"`.
- Use `@Nested` to group related scenarios.

## Specialized Skills (Backend)

- `abrolhos-new-entity`: Scaffold a complete domain entity (entities, repo, JPA, service, DTOs, controller, migration).
- `abrolhos-new-migration`: Create a new Flyway migration with proper timestamp naming.
- `abrolhos-add-cache`: Add Redis caching with `@Cacheable` and `@CacheEvict`.
