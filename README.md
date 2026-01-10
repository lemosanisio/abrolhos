# Abrolhos

Abrolhos is a modern, lightweight blog engine and content management system built with Kotlin and Spring Boot. It provides a robust REST API for managing posts, categories, and tags, with a focus on domain-driven design and performance.

## 🚀 Features

- **Post Management**: Create, read, and list blog posts with support for slugs and status (`DRAFT`, `PUBLISHED`, `SCHEDULED`, `ARCHIVED`).
- **Categorization**: Organize content using categories and tags with automatic slug generation.
- **Domain-Driven Design**: Uses value classes and ULIDs for strong typing and efficient identification.
- **RESTful API**: Fully documented API using SpringDoc OpenAPI (Swagger).
- **Database Migrations**: Automatic schema management with Flyway.
- **Modern Stack**: Built with Kotlin 1.9, Spring Boot 3.4, and Java 21.
- **Docker Ready**: Easy database setup using Docker Compose.

## 🛠️ Technologies

- **Language**: [Kotlin](https://kotlinlang.org/)
- **Framework**: [Spring Boot 3.4](https://spring.io/projects/spring-boot)
- **Database**: [PostgreSQL 16](https://www.postgresql.org/)
- **Persistence**: Spring Data JPA / Hibernate
- **Migrations**: [Flyway](https://flywaydb.org/)
- **Security**: Spring Security (JWT ready)
- **Documentation**: [SpringDoc OpenAPI](https://springdoc.org/)
- **Static Analysis**: [Detekt](https://detekt.dev/)
- **Utility**: [ULID Kotlin](https://github.com/asynkron/ulid-kotlin)

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

- `DB_URL`: JDBC URL for PostgreSQL
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `JWT_SECRET`: Secret key for JWT signing

### 3. Run the Application

```bash
./gradlew bootRun
```

The application will be available at `http://localhost:8080`.

## 📖 API Documentation

Once the application is running, you can access the interactive API documentation at:

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec**: `http://localhost:8080/v3/api-docs`

## 🧪 Testing

Run the test suite using Gradle:

```bash
./gradlew test
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

## 📂 Project Structure

- `src/main/kotlin`: Application source code.
    - `application`: Web controllers, DTOs, and configuration.
    - `domain`: Core business logic, entities, value objects, and repository interfaces.
    - `infrastructure`: Persistence implementations (PostgreSQL) and external integrations.
- `src/main/resources`: Configuration files (`application.yml`) and database migrations (`db/migration`).
- `container`: Docker configuration and database initialization scripts (`init.sql`).

## ⚖️ License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.
