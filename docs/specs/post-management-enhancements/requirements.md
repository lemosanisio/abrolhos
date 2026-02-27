# Requirements Document: Post Management Enhancements

## Introduction

This specification defines enhancements to the Abrolhos blog post management system to enable full CRUD operations on posts. The current system only supports creating and viewing published posts. This enhancement adds the ability to edit posts, delete posts (soft delete), view unpublished posts by their authors, and automatically publish scheduled posts via a background job.

The system uses a Kotlin/Spring Boot 3.4 application with PostgreSQL 16, following hexagonal architecture with Domain-Driven Design principles. Posts already support soft deletes through the BaseEntity infrastructure, and the system has existing audit logging, rate limiting, caching, and metrics capabilities.

## Glossary

- **Post_Management_System**: The blog post subsystem within the Abrolhos application
- **Author**: A user who has created one or more posts
- **Post_Owner**: The user who created a specific post (the author of that post)
- **Authenticated_User**: A user who has successfully authenticated with valid credentials
- **ADMIN**: A user with the ADMIN role who has elevated privileges
- **Slug**: A URL-friendly unique identifier for a post (e.g., "my-first-post")
- **Soft_Delete**: Marking a record as deleted by setting deletedAt timestamp without removing from database
- **Cache_Invalidation**: Removing or updating cached data when the underlying data changes
- **Scheduled_Post**: A post with status SCHEDULED and a publishedAt timestamp in the future
- **Background_Job**: An automated task that runs periodically without user interaction
- **Ownership_Validation**: Verifying that the authenticated user is the author of a post or has ADMIN role
- **Slug_Regeneration**: Creating a new slug when a post title changes
- **Audit_Event**: A logged record of a significant system action for compliance and debugging

## Requirements

### Requirement 1: Edit Post

**User Story:** As an author, I want to edit my posts, so that I can update content, fix errors, and change metadata after initial creation.

#### Acceptance Criteria

1. WHEN an authenticated user requests to edit a post THEN THE Post_Management_System SHALL verify the user is the Post_Owner or has ADMIN role
2. WHEN a post title is updated THEN THE Post_Management_System SHALL regenerate the slug from the new title
3. WHEN a regenerated slug conflicts with an existing post slug THEN THE Post_Management_System SHALL append a numeric suffix to ensure uniqueness
4. WHEN a post is successfully updated THEN THE Post_Management_System SHALL update the updatedAt timestamp to the current time
5. WHEN a post slug changes THEN THE Post_Management_System SHALL invalidate caches for both the old slug and the new slug
6. WHEN a post is updated THEN THE Post_Management_System SHALL invalidate the post summaries cache
7. WHEN a post is successfully updated THEN THE Post_Management_System SHALL record a metric for post updates
8. WHEN a post is successfully updated THEN THE Post_Management_System SHALL create an Audit_Event logging the update operation
9. WHEN an unauthorized user attempts to edit a post THEN THE Post_Management_System SHALL reject the request with a generic error message
10. WHEN a post update request includes valid title, content, status, category, and tags THEN THE Post_Management_System SHALL update all specified fields

### Requirement 2: Delete Post

**User Story:** As an author, I want to delete my posts, so that I can remove content I no longer want published.

#### Acceptance Criteria

1. WHEN an authenticated user requests to delete a post THEN THE Post_Management_System SHALL verify the user is the Post_Owner or has ADMIN role
2. WHEN a post is deleted THEN THE Post_Management_System SHALL perform a Soft_Delete by setting the deletedAt timestamp
3. WHEN a post is soft deleted THEN THE Post_Management_System SHALL exclude it from all queries due to BaseEntity @SQLRestriction
4. WHEN a post is successfully deleted THEN THE Post_Management_System SHALL invalidate the cache for that post slug
5. WHEN a post is successfully deleted THEN THE Post_Management_System SHALL invalidate the post summaries cache
6. WHEN a post is successfully deleted THEN THE Post_Management_System SHALL record a metric for post deletions
7. WHEN a post is successfully deleted THEN THE Post_Management_System SHALL create an Audit_Event logging the delete operation
8. WHEN an unauthorized user attempts to delete a post THEN THE Post_Management_System SHALL reject the request with a generic error message

### Requirement 3: View Author's Own Unpublished Posts

**User Story:** As an author, I want to view my draft and scheduled posts, so that I can review and manage my unpublished content.

#### Acceptance Criteria

1. WHEN an authenticated user requests a post by slug THEN THE Post_Management_System SHALL return the post if it is PUBLISHED
2. WHEN an authenticated user requests a post by slug THEN THE Post_Management_System SHALL return the post if the user is the Post_Owner regardless of status
3. WHEN an ADMIN requests a post by slug THEN THE Post_Management_System SHALL return the post regardless of status or ownership
4. WHEN a post with status DRAFT or SCHEDULED is retrieved THEN THE Post_Management_System SHALL NOT cache the response
5. WHEN an authenticated user who is not the Post_Owner requests an unpublished post THEN THE Post_Management_System SHALL reject the request with a generic error message

### Requirement 4: Scheduled Post Publishing

**User Story:** As an author, I want posts with SCHEDULED status to automatically publish at their scheduled time, so that I can prepare content in advance.

#### Acceptance Criteria

1. THE Post_Management_System SHALL run a Background_Job at a configurable interval to check for Scheduled_Posts ready to publish
2. WHEN the Background_Job runs THEN THE Post_Management_System SHALL query for posts with status SCHEDULED and publishedAt less than or equal to current time
3. WHEN a Scheduled_Post is found ready to publish THEN THE Post_Management_System SHALL update its status to PUBLISHED
4. WHEN a Scheduled_Post is published THEN THE Post_Management_System SHALL set publishedAt to current time if it is null
5. WHEN a Scheduled_Post is published THEN THE Post_Management_System SHALL invalidate the cache for that post slug
6. WHEN a Scheduled_Post is published THEN THE Post_Management_System SHALL invalidate the post summaries cache
7. WHEN a Scheduled_Post is published THEN THE Post_Management_System SHALL record a metric for auto-published posts
8. WHEN a Scheduled_Post is published THEN THE Post_Management_System SHALL create an Audit_Event logging the auto-publish operation
9. WHEN the Background_Job encounters an error processing a post THEN THE Post_Management_System SHALL log the error and continue processing remaining posts

### Requirement 5: Repository Enhancements

**User Story:** As a developer, I want enhanced repository methods, so that I can implement the new post management features.

#### Acceptance Criteria

1. THE Post_Management_System SHALL provide a repository method to find a post by slug regardless of status
2. THE Post_Management_System SHALL provide a repository method to find a post by slug and author ID
3. THE Post_Management_System SHALL provide a repository method to update an existing post
4. THE Post_Management_System SHALL provide a repository method to delete a post using BaseEntity soft delete
5. THE Post_Management_System SHALL provide a repository method to find all Scheduled_Posts with publishedAt less than or equal to a given timestamp

### Requirement 6: Security and Authorization

**User Story:** As a system administrator, I want strict authorization controls on post modifications, so that users can only modify their own content unless they are administrators.

#### Acceptance Criteria

1. WHEN performing Ownership_Validation THEN THE Post_Management_System SHALL allow the operation if the user is the Post_Owner
2. WHEN performing Ownership_Validation THEN THE Post_Management_System SHALL allow the operation if the user has ADMIN role
3. WHEN Ownership_Validation fails THEN THE Post_Management_System SHALL return a generic error message to prevent information disclosure
4. THE Post_Management_System SHALL apply rate limiting to edit and delete endpoints
5. THE Post_Management_System SHALL audit all edit and delete operations with user identity and timestamp

### Requirement 7: Slug Management

**User Story:** As a developer, I want robust slug generation and conflict resolution, so that post URLs remain unique and valid.

#### Acceptance Criteria

1. WHEN generating a slug from a title THEN THE Post_Management_System SHALL convert to lowercase, remove special characters, and replace spaces with hyphens
2. WHEN a generated slug already exists THEN THE Post_Management_System SHALL append a numeric suffix starting with -2
3. WHEN checking for slug conflicts THEN THE Post_Management_System SHALL exclude the current post being updated
4. WHEN a slug is regenerated THEN THE Post_Management_System SHALL validate it against PostSlug value class constraints

### Requirement 8: Cache Management

**User Story:** As a developer, I want proper cache invalidation on post updates, so that users always see current data.

#### Acceptance Criteria

1. WHEN a post is updated with a new slug THEN THE Post_Management_System SHALL evict both old and new slug entries from the postBySlug cache
2. WHEN a post is updated, deleted, or auto-published THEN THE Post_Management_System SHALL evict all entries from the postSummaries cache
3. WHEN retrieving an unpublished post THEN THE Post_Management_System SHALL NOT cache the result

### Requirement 9: Metrics and Observability

**User Story:** As a system operator, I want metrics for post management operations, so that I can monitor system usage and performance.

#### Acceptance Criteria

1. WHEN a post is updated THEN THE Post_Management_System SHALL increment a post_updates counter metric
2. WHEN a post is deleted THEN THE Post_Management_System SHALL increment a post_deletions counter metric
3. WHEN a post is auto-published THEN THE Post_Management_System SHALL increment a post_auto_published counter metric
4. WHEN the Background_Job runs THEN THE Post_Management_System SHALL record the execution time as a metric
