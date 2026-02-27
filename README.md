# Abrolhos

Abrolhos is a modern, lightweight blog engine and content management system built with Kotlin and Spring Boot. It provides a robust REST API for managing posts, categories, and tags, with a focus on domain-driven design and performance.

## 🚀 Features

- **TOTP-Only Authentication**: Secure passwordless authentication using Time-based One-Time Passwords (TOTP) with invitation-based user provisioning.
- **Post Management**: Create, read, and list blog posts with support for slugs and status (`DRAFT`, `PUBLISHED`, `SCHEDULED`, `ARCHIVED`).
- **Categorization**: Organize content using categories and tags with automatic slug generation.
- **Domain-Driven Design**: Uses value classes (inline classes) and ULIDs for strong typing and efficient identification.
- **RESTful API**: Fully documented API using SpringDoc OpenAPI (Swagger).
- **Database Migrations**: Automatic schema management with Flyway.
- **JWT Authentication**: Stateless session management with JWT tokens.
- **Property-Based Testing**: Comprehensive test suite including property-based tests for TOTP security.
- **Modern Stack**: Built with Kotlin 1.9, Spring Boot 3.4, and Java 21.
- **Docker Ready**: Easy database setup using Docker Compose.
- **Integration Testing**: Uses Testcontainers to spin up isolated PostgreSQL and Redis containers.
- **Observability**: Exposes Micrometer Prometheus metrics and custom health probes via Actuator, with structured JSON logging natively matching VictoriaLogs.

## 🛠️ Technologies

- **Language**: [Kotlin 1.9](https://kotlinlang.org/)
- **Framework**: [Spring Boot 3.4](https://spring.io/projects/spring-boot)
- **Database**: [PostgreSQL 16](https://www.postgresql.org/)
- **Persistence**: Spring Data JPA / Hibernate
- **Migrations**: [Flyway](https://flywaydb.org/)
- **Security**: Spring Security with JWT ([Auth0 Java JWT](https://github.com/auth0/java-jwt))
- **TOTP**: [Kotlin OTP](https://github.com/marcelkliemannel/kotlin-onetimepassword)
- **Documentation**: [SpringDoc OpenAPI](https://springdoc.org/)
- **Monitoring**: Micrometer Prometheus Registry & Logstash Encoder
- **Testing**: JUnit 5, Kotest, MockK, SpringMockK, Testcontainers
- **Property-Based Testing**: [Kotest Property](https://kotest.io/docs/proptest/property-based-testing.html)
- **Static Analysis**: [Detekt](https://detekt.dev/)
- **Identifiers**: [ULID Kotlin](https://github.com/aallam/ulid-kotlin)

## 📋 Prerequisites

- **Java 21** or higher
- **Docker** and **Docker Compose** (for database)
- **Gradle** (optional, uses wrapper)

## 🏃 Getting Started

### 1. Database Setup

Use Docker Compose to start the PostgreSQL database:

```bash
docker-compose -f container/docker-compose.yml up -d
```

This will also seed the database with an admin user (`adminuser`) and some initial categories and tags.

### 2. Environment Configuration

The application uses environment variables for configuration. You can find a template in `local.env`. Ensure these variables are set in your environment or IDE:

**Required Variables**:
- `DB_URL`: JDBC URL for PostgreSQL (e.g., `jdbc:postgresql://localhost:5432/abrolhos`)
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `JWT_SECRET`: Secret key for JWT signing (minimum 256 bits recommended)

**Optional Variables**:
- `ALLOWED_ORIGINS`: Comma-separated list of allowed CORS origins (default: `*`)
- `RATE_LIMIT_ENABLED`: Enable rate limiting (default: `false`)
- `RATE_LIMIT_CAPACITY`: Rate limit capacity (default: `10`)
- `RATE_LIMIT_REFILL_TOKENS`: Tokens to refill per period (default: `10`)
- `RATE_LIMIT_REFILL_DURATION_SECONDS`: Refill period in seconds (default: `60`)

**Example `local.env`**:
```bash
DB_URL=jdbc:postgresql://localhost:5432/abrolhos
DB_USERNAME=abrolhos
DB_PASSWORD=abrolhos
JWT_SECRET=your-secret-key-here-minimum-256-bits
ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

### 3. Run the Application

```bash
./gradlew bootRun
```

The application will be available at `http://localhost:8080`.

## 📖 API Documentation

Once the application is running, you can access the interactive API documentation at:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`

### Available Endpoints

**Authentication**:
- `GET /api/auth/invite/{token}` - Validate invitation token and get TOTP provisioning URI
- `POST /api/auth/activate` - Activate account with invitation token and TOTP code
- `POST /api/auth/login` - Login with username and TOTP code

**Posts** (some require authentication):
- `GET /api/posts` - List posts with pagination and filtering
- `GET /api/posts/{slug}` - Get post by slug
- `POST /api/posts` - Create new post (authenticated)

**Health & Monitoring**:
- `GET /actuator/health` - Application health check (with custom DB, Redis, and disk space indicators)
- `GET /actuator/health/liveness` - Dedicated liveness probe
- `GET /actuator/health/readiness` - Dedicated readiness probe
- `GET /actuator/prometheus` - VictoriaMetrics compatible metrics (includes custom business counters)
- `GET /actuator/info` - Application information

## 🧪 Testing

The project includes comprehensive testing with unit tests, integration tests (backed by Testcontainers), and property-based tests.

Run the test suite using Gradle:

```bash
./gradlew test
```

### Test Coverage

- **Unit Tests**: Service layer logic, TOTP generation and validation
- **Property-Based Tests**: 
  - Base32 encoding/decoding round-trip properties
  - Cross-implementation validation with `oathtool`
  - TOTP secret persistence and usage
  - RFC 6238 compliance
- **Integration Tests**: API endpoints (`Auth` and `Posts`), Testcontainer-based db/cache testing, time drift handling

### Running Specific Tests

```bash
# Run only unit tests
./gradlew test --tests "*Test"

# Run only property-based tests
./gradlew test --tests "*PropertyTest"

# Run integration tests (REQUIRES DOCKER to be running)
./gradlew integrationTest
```

## 🧹 Code Quality

Run Detekt to check for code smells and formatting:

```bash
./gradlew detekt
```

To automatically format the code using the `detekt-formatting` ruleset:

```bash
./gradlew detektFormat
```

## 👥 User Provisioning (TOTP Authentication)

Abrolhos uses TOTP-only authentication with manual user provisioning. Users are created via SQL scripts and receive invitation tokens to activate their accounts.

### Creating a New User

1. Open `docs/user-provisioning.sql` in your database client
2. Locate **Script 1: Create a new user and generate an invitation token**
3. Replace the placeholder values:
   - `'your_username'` → actual username (3-20 chars, lowercase)
   - `'ADMIN'` → `'USER'` for regular users, keep `'ADMIN'` for administrators
   - `7` → number of days until invite expires (optional, default is 7)
4. Execute the script
5. Copy the generated `invite_token` from the output
6. Share the token securely with the user

**Example:**
```sql
-- Replace 'your_username' with 'johndoe'
-- Replace 'ADMIN' with 'USER'
-- Keep 7 days expiry
```

### Retrieving an Existing Invite Token

If you need to resend an invitation or check if one exists:

1. Use **Script 2: Retrieve an existing invite token for a user**
2. Replace `'username_here'` with the actual username
3. Execute the script
4. The invite token and expiry status will be displayed

### Managing Invites

The `docs/user-provisioning.sql` file contains additional helper scripts:

- **Script 3**: List all pending invites (for auditing)
- **Script 4**: Delete an expired invite manually
- **Script 5**: Create a new invite for an existing inactive user
- **Script 6**: Check user activation status

### User Activation Flow

1. User receives an invitation token from an administrator
2. User scans the QR code or manually enters the TOTP secret in their authenticator app (Google Authenticator, Authy, etc.)
3. User calls `POST /api/auth/activate` with the invite token and a TOTP code
4. System validates the invite, activates the account, and returns a JWT token
5. User can now log in using `POST /api/auth/login` with their username and TOTP code

### Security Notes

- Invitation tokens are cryptographically secure (64 hex characters, 256 bits of entropy)
- Tokens expire after the configured period (default: 7 days)
- Expired invites are automatically deleted during activation attempts
- Users start as inactive and cannot log in until they complete activation
- TOTP secrets are generated during activation with 160 bits of entropy
- JWT tokens use HMAC256 signing algorithm
- Stateless authentication (no server-side sessions)

### Security Considerations

⚠️ **Current Limitations** (see improvement recommendations):
- TOTP secrets are stored unencrypted in the database
- CORS is configured with wildcard origins by default
- Rate limiting is configured but not enforced by default
- No audit logging for authentication events

For production deployments, consider implementing the security improvements outlined in the project documentation.

## 📂 Project Structure

```
src/main/kotlin/br/dev/demoraes/abrolhos/
├── application/              # Application layer
│   ├── config/              # Spring configuration (SecurityConfig)
│   └── services/            # Application services (AuthService, TotpService, TokenService, PostService)
├── domain/                  # Domain layer (core business logic)
│   ├── entities/            # Domain entities and value objects
│   │   ├── User.kt          # User aggregate with Username, TotpSecret value objects
│   │   ├── Post.kt          # Post aggregate with PostTitle, PostSlug, PostContent
│   │   ├── Category.kt      # Category entity
│   │   ├── Tag.kt           # Tag entity
│   │   └── Invite.kt        # Invitation entity with InviteToken
│   ├── exceptions/          # Domain exceptions (AuthExceptions)
│   └── repository/          # Repository interfaces
└── infrastructure/          # Infrastructure layer
    ├── persistence/         # Database implementations
    │   ├── entities/        # JPA entities (UserEntity, PostEntity, etc.)
    │   └── *RepositoryImpl.kt
    └── web/                 # Web layer
        ├── controllers/     # REST controllers (AuthController, PostController)
        ├── dto/             # Request/Response DTOs
        ├── filters/         # JWT authentication filter
        └── handlers/        # Global exception handler

src/main/resources/
├── application.yml          # Application configuration
└── db/migration/            # Flyway database migrations

src/test/kotlin/             # Test suite
├── application/services/    # Service tests including property-based tests
└── integration/             # Integration tests

container/
├── docker-compose.yml       # PostgreSQL container setup
└── init.sql                 # Database initialization and seed data

docs/
├── user-provisioning.sql    # User provisioning scripts
└── user-provisioning-quick-reference.md
```

## 🏗️ Architecture & Design

### Layered Architecture

The application follows a **Hexagonal Architecture** (Ports and Adapters) with clear separation of concerns:

- **Domain Layer**: Pure business logic with no framework dependencies
- **Application Layer**: Use cases and application services
- **Infrastructure Layer**: Framework-specific implementations (JPA, Spring Security, REST)

### Domain-Driven Design

- **Value Objects**: Inline classes for type safety (`Username`, `TotpSecret`, `PostSlug`, etc.)
- **Aggregates**: `User`, `Post`, `Category`, `Tag` with clear boundaries
- **Repository Pattern**: Domain repositories with infrastructure implementations
- **Domain Events**: (Future enhancement)

### Key Design Decisions

1. **TOTP-Only Authentication**: No passwords to manage, reduced attack surface
2. **Invitation-Based Provisioning**: Manual user creation for controlled access
3. **Value Objects**: Strong typing prevents primitive obsession
4. **ULIDs**: Sortable, URL-safe identifiers with timestamp information
5. **Stateless JWT**: Scalable authentication without server-side sessions

## ⚖️ License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.
