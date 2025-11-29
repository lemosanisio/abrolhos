Blog Engine - tasks.md

This file breaks down the design.md into high-level, buildable tasks.

1. 🏗️ Setup & API-First Contract

[ ] 1.1: Initialize Project: Create the Spring Boot project with Kotlin, Web, Data JPA, Security, PostgreSQL, Thymeleaf, and Actuator.

[ ] 1.2: Configure Database: Set up application.properties to connect to PostgreSQL.

[ ] 1.3: Define API Contract: Create the openapi.yml file. Define all paths and schemas for the JSON API.

[ ] 1.4: Configure OpenAPI Generator: Add the openapi-generator plugin to your pom.xml or build.gradle.kts. Configure it to generate DTOs and API interfaces into target/generated-sources.

2. 🏛️ Domain Layer (The Pure Core)

[ ] 2.1: Create Domain Models: Implement the pure Kotlin data classes in domain/model/ (User, Post, Category, Tag, PostStatus).

[ ] 2.2: Define Domain Ports: Implement the interfaces in domain/ports/ (UserRepository, PostRepository, etc.).

3. 🔌 Infrastructure: Persistence Adapter

[ ] 3.1: Create JPA Entities: Implement the @Entity classes in resources/persistence/model/ (UserEntity, PostEntity, etc.).

[ ] 3.2: Create Mappers: Implement mappers (Domain <-> Entity).

[ ] 3.3: Create JPA Repositories: Implement the Spring Data JpaRepository interfaces.

[ ] 3.4: Implement Port Adapters: Create the adapter classes (e.g., PostRepositoryAdapter) that implement the domain ports.

4. 🧠 Application Layer (Use Cases)

[ ] 4.1: Generate API Code: Run the openapi-generator (e.g., mvn generate-sources) to create the DTOs and API interfaces.

[ ] 4.2: Implement Application Services: Create @Service classes (e.g., PostService) that depend on the domain ports.

[ ] 4.3: Implement Use Cases: Add public methods to the services (e.g., createPost, getPublishedPostBySlug).

5. 🔒 Infrastructure: Security Adapter (JWT)

[ ] 5.1: Implement UserDetailsService: Create UserDetailsServiceImpl that uses the UserRepository port.

[ ] 5.2: Create JWT Utilities: Implement JwtTokenProvider and JwtAuthenticationFilter.

[ ] 5.3: Configure Spring Security: Create the SecurityConfig to protect admin paths and define the password encoder.

6. 🚪 Infrastructure: Web Adapters (API & HTMX)

[ ] 6.1: Implement JSON API:

[ ] Create AuthController for the /login endpoint.

[ ] Create the PostController (and others) to implement the auto-generated OpenAPI interfaces (e.g., PostsApi).

[ ] Connect controller methods to the Application Services.

[ ] 6.2: Implement HTMX UI:

[ ] Create Thymeleaf template files (layouts, fragments).

[ ] Add the HTMX-specific methods (like getPostAsHtmx) to the unified controllers.

[ ] Connect these methods to the Application Services.

7. 🧪 Polish & Validation

[ ] 7.1: Add Validation: Add jakarta.validation annotations to the openapi.yml schemas and re-generate.

[ ] 7.2: Add Health Check: Enable Spring Actuator.

[ ] 7.3: Add Unit Tests: Write unit tests for the Application Services.