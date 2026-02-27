# Implementation Plan: Post Management Enhancements

## Overview

This implementation plan breaks down the post management enhancements into discrete, incremental tasks. Each task builds on previous work and includes testing to validate correctness early. The implementation follows the hexagonal architecture pattern, starting with domain layer changes, then application services, and finally infrastructure components.

## Tasks

- [ ] 1. Extend domain layer with new repository methods and Post entity helpers
  - Add new methods to PostRepository interface: findBySlug, findBySlugAndAuthorId, findScheduledPostsReadyToPublish, delete
  - Add isOwnedBy and withUpdatedFields helper methods to Post entity
  - _Requirements: 1.1, 1.4, 1.10, 2.1, 2.2, 4.2, 5.1, 5.2, 5.3, 5.4, 5.5_

- [ ]* 2. Write property test for Post entity helper methods
  - **Property 4: UpdatedAt Timestamp on Modification**
  - **Property 5: Field Updates Persistence**
  - _Requirements: 1.4, 1.10_

- [ ] 3. Implement repository layer enhancements
  - [ ] 3.1 Add new query methods to PostRepositoryPostgresql JPA interface
    - Add findBySlug, findBySlugAndAuthorId, findByStatusAndPublishedAtLessThanEqual
    - _Requirements: 5.1, 5.2, 5.5_
  
  - [ ] 3.2 Implement new methods in PostRepositoryImpl
    - Implement findBySlug, findBySlugAndAuthorId, findScheduledPostsReadyToPublish, delete
    - Map between domain Post and PostEntity
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
  
  - [ ]* 3.3 Write property test for scheduled post query
    - **Property 8: Scheduled Post Query Correctness**
    - _Requirements: 4.2_

- [ ] 4. Enhance MetricsService with new counters
  - Add postUpdates, postDeletions, postAutoPublished counters
  - Add scheduledPublishingJobTimer
  - Add recordPostUpdate, recordPostDeletion, recordPostAutoPublished, recordScheduledPublishingJobTime methods
  - _Requirements: 9.1, 9.2, 9.3, 9.4_

- [ ] 5. Enhance AuditLogger with post management events
  - Add logPostUpdate, logPostDeletion, logPostAutoPublished methods
  - Include postId, username, slug, and relevant details in audit events
  - _Requirements: 1.8, 2.7, 4.8, 6.5_

- [ ] 6. Implement slug generation and conflict resolution in PostService
  - [ ] 6.1 Add generateUniqueSlug private method
    - Generate base slug from title (lowercase, remove special chars, replace spaces with hyphens)
    - Check for conflicts and append numeric suffix if needed
    - Exclude current post ID when checking conflicts
    - _Requirements: 1.2, 1.3, 7.1, 7.2, 7.3, 7.4_
  
  - [ ]* 6.2 Write property tests for slug generation
    - **Property 2: Slug Generation from Title**
    - **Property 3: Slug Conflict Resolution**
    - _Requirements: 1.2, 1.3, 7.1, 7.2, 7.4_
  
  - [ ]* 6.3 Write unit tests for slug edge cases
    - Test excluding current post during conflict check
    - Test special characters and unicode handling
    - _Requirements: 7.3_

- [ ] 7. Implement updatePost method in PostService
  - [ ] 7.1 Add updatePost method with authorization check
    - Verify user is post owner or ADMIN
    - Handle slug regeneration if title changes
    - Find or create category and tags if provided
    - Update post fields using withUpdatedFields
    - Invalidate caches, record metrics, audit log
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10, 6.1, 6.2, 6.3_
  
  - [ ]* 7.2 Write property test for authorization
    - **Property 1: Authorization for Post Modifications**
    - _Requirements: 1.1, 2.1, 6.1, 6.2_
  
  - [ ]* 7.3 Write unit tests for update scenarios
    - Test updating individual fields
    - Test updating multiple fields at once
    - Test unauthorized access returns generic error
    - _Requirements: 1.9, 1.10, 6.3_

- [ ] 8. Implement deletePost method in PostService
  - [ ] 8.1 Add deletePost method with authorization check
    - Verify user is post owner or ADMIN
    - Call repository delete (soft delete via BaseEntity)
    - Invalidate caches, record metrics, audit log
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 6.1, 6.2, 6.3_
  
  - [ ]* 8.2 Write property test for soft delete
    - **Property 6: Soft Delete Sets DeletedAt**
    - _Requirements: 2.2, 2.3_
  
  - [ ]* 8.3 Write unit tests for delete scenarios
    - Test unauthorized access returns generic error
    - Test deleted posts don't appear in queries
    - _Requirements: 2.3, 2.8, 6.3_

- [ ] 9. Implement findBySlugForUser method in PostService
  - [ ] 9.1 Add findBySlugForUser method with visibility rules
    - Return published posts to everyone
    - Return unpublished posts to owner or ADMIN
    - Return generic error for unauthorized access
    - Don't cache unpublished posts
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_
  
  - [ ]* 9.2 Write property test for visibility rules
    - **Property 7: Post Visibility Rules**
    - _Requirements: 3.1, 3.2, 3.3, 3.5_
  
  - [ ]* 9.3 Write unit tests for visibility edge cases
    - Test unauthenticated access to unpublished posts
    - Test generic error messages
    - _Requirements: 3.5, 6.3_

- [ ] 10. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 11. Create ScheduledPublishingService
  - [ ] 11.1 Implement ScheduledPublishingService with @Scheduled method
    - Query for scheduled posts ready to publish
    - Update status to PUBLISHED and set publishedAt
    - Handle errors gracefully and continue processing
    - Invalidate caches, record metrics, audit log
    - Record job execution time
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7, 4.8, 4.9_
  
  - [ ]* 11.2 Write property test for scheduled publishing
    - **Property 9: Scheduled Post Publishing State Transition**
    - _Requirements: 4.3, 4.4_
  
  - [ ]* 11.3 Write property test for error resilience
    - **Property 10: Error Resilience in Batch Processing**
    - _Requirements: 4.9_
  
  - [ ]* 11.4 Write unit tests for scheduled publishing
    - Test job runs at configured interval
    - Test only posts with publishedAt <= now are published
    - Test error logging
    - _Requirements: 4.1, 4.2, 4.9_

- [ ] 12. Add configuration property for scheduled publishing interval
  - Add app.scheduled-publishing.interval property to application.yml
  - Default to 60000ms (1 minute)
  - Document configuration in README or config comments
  - _Requirements: 4.1_

- [ ] 13. Create UpdatePostRequest DTO
  - Create UpdatePostRequest data class with optional fields
  - Add validation annotations if needed
  - Place in infrastructure/web/dto/request package
  - _Requirements: 1.10_

- [ ] 14. Add PUT endpoint to PostsController
  - [ ] 14.1 Implement updatePost endpoint
    - Extract user role from authentication
    - Call postService.updatePost
    - Return updated post response
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10_
  
  - [ ]* 14.2 Write integration tests for PUT endpoint
    - Test successful update as owner
    - Test successful update as admin
    - Test unauthorized update returns 403
    - Test rate limiting
    - _Requirements: 1.1, 1.9, 6.4_

- [ ] 15. Add DELETE endpoint to PostsController
  - [ ] 15.1 Implement deletePost endpoint
    - Extract user role from authentication
    - Call postService.deletePost
    - Return 204 No Content
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8_
  
  - [ ]* 15.2 Write integration tests for DELETE endpoint
    - Test successful delete as owner
    - Test successful delete as admin
    - Test unauthorized delete returns 403
    - Test rate limiting
    - _Requirements: 2.1, 2.8, 6.4_

- [ ] 16. Modify GET /{slug} endpoint to support unpublished posts
  - [ ] 16.1 Update getPostBySlug to call findBySlugForUser
    - Extract user role from authentication (nullable)
    - Pass authentication info to service
    - Return post if visible to user
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_
  
  - [ ]* 16.2 Write integration tests for GET endpoint visibility
    - Test published post visible to everyone
    - Test unpublished post visible to owner
    - Test unpublished post visible to admin
    - Test unpublished post not visible to other users
    - Test unpublished posts not cached
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

- [ ] 17. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 18. Create Kotest property test generators
  - Create Arb.post() generator for random posts
  - Create Arb.user() generator for random users with roles
  - Create Arb.postTitle(), Arb.postContent() generators
  - Create Arb.category(), Arb.tag() generators
  - Place in test utilities package
  - _Requirements: All property tests_

- [ ]* 19. Write comprehensive integration tests
  - Test end-to-end flow: create → update → delete
  - Test cache invalidation on all operations
  - Test metrics recording for all operations
  - Test audit logging for all operations
  - Test scheduled publishing job execution
  - _Requirements: 1.5, 1.6, 1.7, 1.8, 2.4, 2.5, 2.6, 2.7, 4.5, 4.6, 4.7, 4.8, 6.5, 8.1, 8.2, 8.3, 9.1, 9.2, 9.3, 9.4_

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Integration tests validate infrastructure concerns (caching, metrics, audit logging)
- Authorization is checked at service layer before any modification
- All errors use generic messages to prevent information disclosure
- Soft delete is handled automatically by BaseEntity infrastructure
