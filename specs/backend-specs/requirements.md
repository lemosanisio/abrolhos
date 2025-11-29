Blog Engine - requirements.md

This document outlines the functional and non-functional requirements for the blog engine backend.

1. Functional Requirements (FR)

FR1: User & Authentication System

FR1.1: User Entity: The system must have a User entity.

User fields: id, username, hashedPassword, role.

FR1.2: Roles: The system will initially support one ROLE_ADMIN. The design must be expandable to support other roles (e.g., ROLE_AUTHOR) in the future.

FR1.3: Authentication: Authentication must be stateless.

A public /login endpoint will accept username and password.

On success, it will return a JSON Web Token (JWT).

FR1.4: Authorization:

All "Read" endpoints (e.g., viewing posts, categories) will be public.

All "Write" endpoints (Create, Update, Delete) must be protected and require a valid JWT from a user with ROLE_ADMIN.

FR2: Post Management

FR2.1: Post Entity: The system must have a Post entity.

Post fields: id, title, content (body), slug (URL-friendly string), publishDate, lastUpdated.

Post relations: author (One-to-Many with User), categories (Many-to-Many), tags (Many-to-Many).

FR2.2: Post Status: A Post must have a status, defined by an enum:

DRAFT: Not publicly visible.

PUBLISHED: Publicly visible.

SCHEDULED: Not publicly visible yet; scheduled for future publication.

ARCHIVED: Not publicly visible; retained for historical record (soft delete).

FR2.3: Slug Generation: The slug should be automatically generated from the title upon creation to ensure it is URL-safe and unique.

FR3: Taxonomy Management (Categories & Tags)

FR3.1: Category Entity: The system must have a Category entity.

Category fields: id, name, slug.

FR3.2: Tag Entity: The system must have a Tag entity.

Tag fields: id, name, slug.

FR3.3: Relationships:

A Post can have many Categories. A Category can have many Posts (Many-to-Many).

A Post can have many Tags. A Tag can have many Posts (Many-to-Many).

FR3.4: Management: The system must provide protected (Admin-only) endpoints for CRUD operations on Categories and Tags.

FR4: Public API (Read Operations)

The system must provide public, unauthenticated endpoints for:

GET /posts: Get a paginated list of all PUBLISHED posts.

GET /posts/{slug}: Get a single PUBLISHED post by its slug.

GET /categories: Get a list of all Categories.

GET /categories/{slug}/posts: Get paginated PUBLISHED posts belonging to a specific category.

GET /tags: Get a list of all Tags.

GET /tags/{slug}/posts: Get paginated PUBLISHED posts belonging to a specific tag.

FR5: Admin API (Write Operations)

The system must provide protected, authenticated endpoints for:

POST /posts: Create a new post (can be saved as DRAFT).

PUT /posts/{id}: Update an existing post.

DELETE /posts/{id}: Delete a post.

GET /admin/posts: Get a list of all posts, regardless of status.

CRUD operations for Categories and Tags.

2. Non-Functional Requirements (NFR)

NFR1: Architecture: The system must be built using Hexagonal Architecture (Ports and Adapters) and Domain-Driven Design (DDD) principles.

The domain layer must be pure and contain no Spring Boot or Hibernate dependencies.

All external communication (database, web) must be handled by adapters implementing ports (interfaces) defined in the domain.

NFR2: Expandability: The architecture must easily accommodate future features, including a commenting system, multiple user roles, and new interfaces (like native mobile apps).

NFR3: Technology:

Language: Kotlin (Java 21)

Framework: Spring Boot 3+ (with Thymeleaf for HTMX)

Database: PostgreSQL

Persistence: Spring Data JPA (with Hibernate).

NFR4: API: The system will serve two representations from the same endpoints:

application/json (for React, mobile apps)

text/html (for HTMX)

NFR5: Validation: All incoming data (DTOs) from the web adapter must be validated (e.g., using jakarta.validation).

NFR6: Observability: The system must expose a /actuator/health endpoint via Spring Actuator.

NFR7: API-First Design: The JSON API must be formally defined using the OpenAPI 3.0 specification (in an openapi.yml file). This contract is the single source of truth for all API endpoints and DTOs.