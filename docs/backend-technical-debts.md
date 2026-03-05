# Backend Technical Debt

This document tracks technical debt and required improvements for the Kotlin/Spring Boot backend, identified during frontend development sessions.

## Markdown Transition (2026-02-28)

With the move to a Markdown editor in the frontend, the following backend changes are required to ensure data integrity and security:

### 1. Content Sanitization
- **Issue**: The backend currently expects and potentially sanitizes HTML.
- **Requirement**: Update the sanitization logic to handle Markdown. It should prevent XSS by escaping literal HTML tags within the Markdown body while allowing valid Markdown syntax to pass through.

### 2. `shortContent` Generation
- **Issue**: `shortContent` (post summaries) is generated on the backend.
- **Requirement**: Ensure the truncation logic for `shortContent` accounts for Markdown characters.
  - Ideally, strip Markdown formatting (e.g., `**`, `###`, `[text](url)`) before generating the summary to avoid "broken" Markdown syntax at the truncation point (e.g., `This is a **bold tex...`).
  - Alternatively, ensure the truncated string is still valid Markdown.

### 3. API Contract Validation
- **Issue**: The `content` field is a generic string.
- **Requirement**: Explicitly document in the API contract (OpenAPI) that `content` now follows the Markdown specification rather than HTML.
