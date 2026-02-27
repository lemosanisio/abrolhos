# Post Management Enhancements and Roadmap

This document outlines the detailed implementation plan for extending the post management capabilities of the Abrolhos blog engine, specifically focusing on editing and deleting posts, along with a roadmap for future enhancements based on current application requirements.

## 1. Goal Description

Enhance the current post management system to allow authors to fully manage the lifecycle of their content. The application relies on a solid Hexagonal Architecture with `BaseEntity` already supporting Soft Deletes.

The core additions will be:
1. **Edit Post (`PUT /api/posts/{slug}`)**: Allows updating the title (which will also update the slug), content, status, category, and tags.
2. **Delete Post (`DELETE /api/posts/{slug}`)**: Triggers a soft delete (leveraging `BaseEntity`'s `@SQLDelete` and `@SQLRestriction`).
3. **View Drafts (`GET /api/posts/{slug}/draft` or similar)**: Allow authors to view their own unpublished posts (Drafts/Scheduled).

### Key Business Rules
*   **Slug Mutability**: The post slug **must** change when the post title is updated.
*   **Authorization**: Strict ownership enforcement. Only the original author of the post (or an Administrator) can edit or delete a post. The system only has `AUTHOR` and `ADMIN` user roles, with no independent reader accounts.

## 2. Proposed Changes

### 2.1 Domain Layer

#### `PostRepository.kt`
*   Add `fun delete(post: Post)` to support post removal.
*   Add `fun findBySlug(slug: String): Post?` to fetch a post regardless of its `status`. This is required so authors can fetch and edit `DRAFT` or `SCHEDULED` posts (the existing `findPublishedBySlug` is too restrictive for administration).

### 2.2 Infrastructure Layer (Persistence)

#### `PostRepositoryImpl.kt`
*   Implement `delete(post: Post)` by mapping to the underlying Spring Data JPA repository's `deleteById()` or `delete()` method. Because `BaseEntity` is annotated with `@SQLDelete`, this will automatically perform a soft delete by setting `deleted_at = CURRENT_TIMESTAMP`.
*   Implement the unfiltered `findBySlug(slug: String)` using the existing JPA method `postRepositoryPostgresql.findBySlug(slug)`.

### 2.3 Application Layer (Service)

#### `PostService.kt`
*   **`updatePost(...)`**:
    *   Fetch the existing post using the internal (unfiltered) `findBySlug`.
    *   Verify ownership: Ensure the `username` attempting the edit matches the post's `author.username.value` (or has ADMIN privileges). Throw an `AccessDeniedException` if unauthorized.
    *   Update the `title`.
    *   Regenerate and update the `slug` based on the new title.
    *   Update `content`, `status`, `category` (finding or creating), and `tags` (finding or creating).
    *   Set `updatedAt = OffsetDateTime.now()`.
    *   Save the entity and record metrics (`metricsService.recordPostUpdate()`).
    *   Ensure appropriate `@CacheEvict` annotations are used or cache management logic is applied to invalidate the old slug and update the new one if necessary.
*   **`deletePost(...)`**:
    *   Fetch the existing post using the internal `findBySlug`.
    *   Verify ownership as described above.
    *   Call `postRepository.delete(post)`.
    *   Record metrics (`metricsService.recordPostDeletion()`).
*   **`getAuthorPost(...)`**:
    *   A method to fetch a post by slug for an author, verifying ownership before returning, allowing them to view `DRAFT` or `SCHEDULED` statuses securely.

### 2.4 Web Layer (Controllers & DTOs)

#### `UpdatePostRequest.kt` (New)
*   Create a specific Request DTO for edits containing the fields: `title`, `content`, `status`, `categoryName`, and `tagNames`.

#### `PostsController.kt`
*   **`@PutMapping("/{slug}")`**: Secured endpoint mapped to `PostService.updatePost`. Returns the updated post data.
*   **`@DeleteMapping("/{slug}")`**: Secured endpoint mapped to `PostService.deletePost`. Returns a `204 No Content` status.
*   **Draft Viewing**: Add a secured endpoint (e.g., `@GetMapping("/me/{slug}")` or utilize the existing structure with proper security context checks) that calls `PostService.getAuthorPost()` to allow authors to retrieve their unpublished work.

## 3. Future Roadmap

Based on the current analysis, the following feature should be added to the development roadmap:

### 3.1 Post Scheduling Logic
While the `PostStatus` enum already includes `SCHEDULED`, the background logic to automatically transition these posts to `PUBLISHED` when their scheduled time arrives is currently missing.
*   **Implementation Idea**: Introduce a Spring `@Scheduled` task that periodically scans for posts with `status = SCHEDULED` and a `publishedAt` timestamp less than or equal to the current time, updating their status to `PUBLISHED` and clearing necessary caches.
