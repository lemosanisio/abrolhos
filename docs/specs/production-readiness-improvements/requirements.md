# Requirements Document

## Introduction

This document specifies production readiness improvements for the Abrolhos backend application. The scope is focused on critical security hardening, observability enhancements, and code quality improvements for a single-instance, self-hosted deployment using VictoriaMetrics, VictoriaLogs, and Zipkin.

## Glossary

- **Application**: The Abrolhos backend Spring Boot application
- **Correlation_ID**: A unique identifier that tracks a request through the system
- **MDC**: Mapped Diagnostic Context - a thread-local storage mechanism for logging context
- **Actuator**: Spring Boot Actuator endpoints for application monitoring and management
- **JWT_Secret**: The secret key used to sign and verify JSON Web Tokens
- **VictoriaMetrics**: Time-series database for metrics storage and querying
- **Zipkin**: Distributed tracing system for monitoring microservices
- **Micrometer**: Application metrics facade that supports multiple monitoring systems

## Requirements

### Requirement 1: JWT Secret Validation

**User Story:** As a system administrator, I want the application to validate JWT secret strength at startup, so that weak secrets are rejected before the application becomes operational.

#### Acceptance Criteria

1. WHEN the Application starts THEN the Application SHALL validate that the JWT_Secret is at least 32 characters long
2. IF the JWT_Secret is shorter than 32 characters THEN the Application SHALL fail to start and log a clear error message
3. WHEN the JWT_Secret validation fails THEN the Application SHALL exit with a non-zero status code

### Requirement 2: Actuator Endpoint Security

**User Story:** As a security engineer, I want actuator endpoints to require ADMIN role authorization, so that sensitive operational endpoints are protected from unauthorized access.

#### Acceptance Criteria

1. WHEN a request is made to any Actuator endpoint except /health THEN the Application SHALL require ADMIN role authorization
2. WHEN an unauthenticated request is made to a protected Actuator endpoint THEN the Application SHALL return HTTP 401 Unauthorized
3. WHEN an authenticated non-ADMIN user requests a protected Actuator endpoint THEN the Application SHALL return HTTP 403 Forbidden
4. WHEN a request is made to /actuator/health THEN the Application SHALL allow access without authentication

### Requirement 3: Generic Exception Handler

**User Story:** As a developer, I want a catch-all exception handler that includes correlation IDs, so that unexpected errors are logged consistently and can be traced.

#### Acceptance Criteria

1. WHEN an unhandled exception occurs THEN the Application SHALL catch it with a generic exception handler
2. WHEN the generic exception handler processes an exception THEN the Application SHALL log the exception with the Correlation_ID
3. WHEN the generic exception handler processes an exception THEN the Application SHALL return HTTP 500 Internal Server Error
4. WHEN the generic exception handler returns an error response THEN the Application SHALL include the Correlation_ID in the response body

### Requirement 4: Correlation ID Implementation

**User Story:** As a developer, I want correlation IDs to be generated and propagated through the system, so that I can trace requests across logs and services.

#### Acceptance Criteria

1. WHEN a request arrives without a Correlation_ID header THEN the Application SHALL generate a new Correlation_ID
2. WHEN a request arrives with a Correlation_ID header THEN the Application SHALL extract and use that Correlation_ID
3. WHEN a Correlation_ID is available THEN the Application SHALL store it in the MDC
4. WHEN the Application logs a message THEN the Application SHALL include the Correlation_ID from MDC
5. WHEN the Application returns a response THEN the Application SHALL include the Correlation_ID in the response headers
6. WHEN a request completes THEN the Application SHALL clear the Correlation_ID from MDC

### Requirement 5: Micrometer Tracing for Zipkin

**User Story:** As a DevOps engineer, I want distributed tracing with Zipkin using Micrometer Tracing, so that I can monitor request flows and identify performance bottlenecks.

#### Acceptance Criteria

1. WHEN the Application starts THEN the Application SHALL configure Micrometer Tracing for Zipkin integration
2. WHEN a request is processed THEN the Application SHALL create a trace span with trace ID and span ID
3. WHEN a trace span is created THEN the Application SHALL send trace data to Zipkin
4. WHEN trace data is sent THEN the Application SHALL include service name, operation name, and timing information
5. WHERE Zipkin is unavailable THEN the Application SHALL continue operating without failing

### Requirement 6: Enhanced Metrics

**User Story:** As a DevOps engineer, I want custom business metrics exposed to VictoriaMetrics, so that I can monitor application-specific behavior and performance.

#### Acceptance Criteria

1. WHEN a post is created THEN the Application SHALL increment a post creation counter metric
2. WHEN an authentication attempt occurs THEN the Application SHALL increment an authentication attempt counter metric with success/failure tags
3. WHEN a cache hit or miss occurs THEN the Application SHALL increment cache hit/miss counter metrics
4. WHEN metrics are exposed THEN the Application SHALL make them available in Prometheus format for VictoriaMetrics scraping
5. WHEN metrics are recorded THEN the Application SHALL include relevant tags for filtering and aggregation

### Requirement 7: TODO Resolution

**User Story:** As a developer, I want all TODO comments resolved or documented, so that technical debt is tracked and the codebase is production-ready.

#### Acceptance Criteria

1. WHEN the codebase is reviewed THEN the Application SHALL have no unresolved TODO comments
2. WHERE a TODO represents future work THEN the Application SHALL document it in a tracking system and remove the TODO comment
3. WHERE a TODO represents incomplete functionality THEN the Application SHALL complete the functionality before production deployment

### Requirement 8: Debug Secret Logging Removal

**User Story:** As a security engineer, I want all debug logging of sensitive data removed, so that secrets are not exposed in production logs.

#### Acceptance Criteria

1. WHEN the codebase is reviewed THEN the Application SHALL not log JWT tokens, passwords, or API keys
2. WHEN the codebase is reviewed THEN the Application SHALL not log request/response bodies containing sensitive data
3. WHERE logging is necessary for debugging THEN the Application SHALL redact or mask sensitive fields

### Requirement 9: Security Hardening

**User Story:** As a security engineer, I want security best practices implemented, so that the application is hardened against common attacks.

#### Acceptance Criteria

1. WHEN the Application responds to requests THEN the Application SHALL include security headers (X-Content-Type-Options, X-Frame-Options, X-XSS-Protection, Strict-Transport-Security)
2. WHEN an HTTP request is received THEN the Application SHALL redirect to HTTPS in production environments
3. WHEN CORS is configured THEN the Application SHALL validate allowed origins against a whitelist
4. WHEN rate limiting is configured THEN the Application SHALL verify that rate limits are appropriate for production traffic
5. WHEN the Application handles user input THEN the Application SHALL validate and sanitize input to prevent injection attacks
6. WHEN the Application extracts client IP addresses THEN the Application SHALL use trusted proxy headers configured via Spring Boot's forward headers strategy
