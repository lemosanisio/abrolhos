# Project TODO-USER-USER Analysis Report

## Overview
This report analyzes the current `TODO-USER` comments found within the codebase. It segments them by category (Architecture, Knowledge Gaps, Code Quality) and provides recommendations for resolution.

**Total TODO-USERs:** 7

## 1. Architecture & Layering

### **Domain Exceptions**
*   **File:** `src/main/kotlin/br/dev/demoraes/abrolhos/domain/exceptions/AuthExceptions.kt`
    *   **Comment:** `// TODO-USER-USER(Could this one be moved to infrastructure?)`
    *   **Analysis:** These define business rules (e.g., `UserNotFound`, `InvalidInvite`). They belong in the **Domain Layer**. Moving them to Infrastructure would violate dependency rules.
    *   **Recommendation:** **Keep in Domain**. Remove the TODO-USER.

### **Encryption Exceptions**
*   **File:** `src/main/kotlin/br/dev/demoraes/abrolhos/domain/exceptions/EncryptionException.kt`
    *   **Comment:** `// TODO-USER-USER(Could this one be moved to infrastructure?)`
    *   **Analysis:** If the domain defines an `EncryptionService` interface (a Port), the exception is part of that contract.
    *   **Recommendation:** **Keep in Domain**. Remove the TODO-USER.

## 2. Knowledge Gaps & Review Needed

### **Audit Aspect**
*   **File:** `src/main/kotlin/br/dev/demoraes/abrolhos/application/audit/AuditAspect.kt`
    *   **Comment:** `// TODO-USER-USER(I dont know how that audit works, it seemed nice to have when kiro suggested)`
    *   **Analysis:** The implementation (Spring AOP `@Aspect`) intercepts login methods correctly.
    *   **Recommendation:** The code is functional. **Validate logs** and remove the TODO-USER.

### **Global Exception Handler**
*   **File:** `src/main/kotlin/br/dev/demoraes/abrolhos/infrastructure/web/handlers/GlobalExceptionHandler.kt`
    *   **Comment:** `// TODO-USER-USER(This one looks good, but im not used to using exception handlers, will need some thought too)`
    *   **Analysis:** Using `@RestControllerAdvice` is the standard, recommended approach in Spring Boot.
    *   **Recommendation:** **Keep is as.** Remove the TODO-USER.

### **Rate Limiting**
*   **File:** `src/main/kotlin/br/dev/demoraes/abrolhos/infrastructure/web/filters/RateLimitFilter.kt`
    *   **Comment:** `// TODO-USER-USER(Will need to learn more about that too)`
    *   **Analysis:** The class implements `HandlerInterceptor` but is named `Filter`.
    *   **Recommendation:** Functionally correct. Consider renaming to `RateLimitInterceptor` for clarity.

## 3. Code Quality & Future Improvements

### **Error Response Structure**
*   **File:** `src/main/kotlin/br/dev/demoraes/abrolhos/infrastructure/web/dto/response/ErrorResponse.kt`
    *   **Comment:** `// TODO-USER-USER(Can i make it inherit ErrorResponse? Will do some research later, dont know what it does)`
    *   **Analysis:** The author may be referring to Spring's `ProblemDetail` or `ResponseEntity`. The current simple data class is valid.
    *   **Recommendation:** **Keep for now.** Evaluate [RFC 9457](ttps://www.rfc-editor.org/rfc/rfc9457.html) (`ProblemDetail`) for future standardization.

### **JWT Authentication Performance**
*   **File:** `src/main/kotlin/br/dev/demoraes/abrolhos/infrastructure/web/filters/JwtAuthenticationFilter.kt`
    *   **Comment:** `// TODO-USER-USER(I will need some time to think about improvements here)`
    *   **Analysis:** The filter performs a database lookup (`userRepository.findById`) on every request.
    *   **Recommendation:** **Optimize later.** Consider caching (Redis/Caffeine) or stateless JWT validation (if instant revocation checks aren't critical).

## Summary of Actions

| Category | Count | Primary Action |
| :--- | :--- | :--- |
| **Keep/Validate** | 4 | Confirm design choice and remove TODO-USER. |
| **Refactor** | 1 | Rename `RateLimitFilter` to `Interceptor` (optional). |
| **Research** | 2 | Decide on `ErrorResponse` standard & JWT Caching. |
