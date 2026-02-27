# Design Document: Post Management Enhancements

## Overview

This design extends the Abrolhos blog post management system to support full CRUD operations. The current implementation only allows creating and viewing published posts. This enhancement adds:

1. **Edit Post**: Update title, content, status, category, and tags with slug regeneration and conflict resolution
2. **Delete Post**: Soft delete posts using existing BaseEntity infrastructure
3. **View Unpublished Posts**: Allow authors to view their own DRAFT/SCHEDULED posts
4. **Scheduled Publishing**: Background job to auto-publish scheduled posts
5. **Repository Enhancements**: New query methods to support the above features

The design follows the existing hexagonal architecture with clear separation between domain, application, and infrastructure layers. It leverages existing infrastructure for caching (Redis), metrics (Micrometer), audit logging, and soft deletes.

## Architecture

### Layer Responsibilities

**Domain Layer** (`domain/`):
- `PostRepository` interface: Add new methods for finding posts by slug (any status), finding by slug and author, and finding scheduled posts
- `Post` entity: Add methods for updating fields and validating ownership
- No new exceptions needed (use existing `NoSuchElementException` and Spring Security exceptions)

**Application Layer** (`application/services/`):
- `PostService`: Add methods for `updatePost()`, `deletePost()`, `findBySlugForUser()`, and `publishScheduledPosts()`
- `ScheduledPublishingService`: New service with `@Scheduled` method to auto-publish posts
- Coordinate authorization checks, cache invalidation, metrics, and audit logging

**Infrastructure Layer** (`infrastructure/`):
- `PostRepositoryImpl`: Implement new repository methods
- `PostRepositoryPostgresql`: Add JPA query methods
- `PostsController`: Add PUT and DELETE endpoints
- `MetricsService`: Add counters for updates, deletes, and auto-publishes
- `AuditLogger`: Add methods for post update/delete events

### Key Design Decisions

1. **Slug Regeneration**: When title changes, regenerate slug and handle conflicts by appending numeric suffixes (-2, -3, etc.)
2. **Authorization**: Check ownership at service layer before any modification (author or ADMIN)
3. **Cache Strategy**: Invalidate both old and new slugs on update; never cache unpublished posts
4. **Soft Delete**: Use existing BaseEntity @SQLDelete annotation - no custom logic needed
5. **Scheduled Publishing**: Use Spring's @Scheduled with configurable interval (default 1 minute)
6. **Error Messages**: Generic messages on authorization failures to prevent information disclosure

## Components and Interfaces

### 1. Domain Layer Changes

#### PostRepository Interface

```kotlin
interface PostRepository {
    // Existing methods
    fun save(post: Post): Post
    fun findPublishedBySlug(slug: String): Post?
    fun searchSummary(pageable: Pageable, categoryName: String?, tagName: String?, status: PostStatus): Page<PostSummary>
    fun searchSummaryByCursor(cursor: String?, size: Int, status: PostStatus): CursorPage<PostSummary>
    
    // New methods
    fun findBySlug(slug: String): Post?
    fun findBySlugAndAuthorId(slug: String, authorId: ULID): Post?
    fun findScheduledPostsReadyToPublish(now: OffsetDateTime): List<Post>
    fun delete(post: Post)
}
```

#### Post Entity Extensions

Add helper methods to Post entity:

```kotlin
data class Post(...) {
    // Existing companion object and fields
    
    fun isOwnedBy(userId: ULID): Boolean = author.id == userId
    
    fun withUpdatedFields(
        title: PostTitle? = null,
        slug: PostSlug? = null,
        content: PostContent? = null,
        status: PostStatus? = null,
        category: Category? = null,
        tags: Set<Tag>? = null,
        publishedAt: OffsetDateTime? = null
    ): Post {
        return copy(
            title = title ?: this.title,
            slug = slug ?: this.slug,
            content = content ?: this.content,
            status = status ?: this.status,
            category = category ?: this.category,
            tags = tags ?: this.tags,
            publishedAt = publishedAt ?: this.publishedAt,
            updatedAt = OffsetDateTime.now()
        )
    }
}
```

### 2. Application Layer Changes

#### PostService Enhancements

```kotlin
@Service
@Transactional
class PostService(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val metricsService: MetricsService,
    private val auditLogger: AuditLogger
) {
    // Existing methods...
    
    @CacheEvict(value = ["postBySlug", "postSummaries"], allEntries = true)
    fun updatePost(
        slug: String,
        title: String?,
        content: String?,
        status: PostStatus?,
        categoryName: String?,
        tagNames: List<String>?,
        currentUsername: String,
        currentUserRole: Role
    ): Post {
        val post = postRepository.findBySlug(slug)
            ?: throw NoSuchElementException("Post not found")
        
        val currentUser = userRepository.findByUsername(Username(currentUsername))
            ?: throw NoSuchElementException("User not found")
        
        // Authorization check
        if (!post.isOwnedBy(currentUser.id) && currentUserRole != Role.ADMIN) {
            throw AccessDeniedException("Not authorized to edit this post")
        }
        
        // Handle slug regeneration if title changed
        val newSlug = if (title != null && title != post.title.value) {
            generateUniqueSlug(title, post.id)
        } else {
            null
        }
        
        // Find or create category and tags if provided
        val newCategory = categoryName?.let { findOrCreateCategory(it) }
        val newTags = tagNames?.map { findOrCreateTag(it) }?.toSet()
        
        // Update post
        val updatedPost = post.withUpdatedFields(
            title = title?.let { PostTitle(it) },
            slug = newSlug?.let { PostSlug(it) },
            content = content?.let { PostContent(it) },
            status = status,
            category = newCategory,
            tags = newTags,
            publishedAt = if (status == PostStatus.PUBLISHED && post.publishedAt == null) {
                OffsetDateTime.now()
            } else {
                null
            }
        )
        
        val saved = postRepository.save(updatedPost)
        
        // Record metrics and audit
        metricsService.recordPostUpdate()
        auditLogger.logPostUpdate(
            postId = post.id.toString(),
            username = currentUsername,
            oldSlug = post.slug.value,
            newSlug = saved.slug.value
        )
        
        return saved
    }
    
    @CacheEvict(value = ["postBySlug", "postSummaries"], allEntries = true)
    fun deletePost(
        slug: String,
        currentUsername: String,
        currentUserRole: Role
    ) {
        val post = postRepository.findBySlug(slug)
            ?: throw NoSuchElementException("Post not found")
        
        val currentUser = userRepository.findByUsername(Username(currentUsername))
            ?: throw NoSuchElementException("User not found")
        
        // Authorization check
        if (!post.isOwnedBy(currentUser.id) && currentUserRole != Role.ADMIN) {
            throw AccessDeniedException("Not authorized to delete this post")
        }
        
        postRepository.delete(post)
        
        // Record metrics and audit
        metricsService.recordPostDeletion()
        auditLogger.logPostDeletion(
            postId = post.id.toString(),
            username = currentUsername,
            slug = post.slug.value
        )
    }
    
    fun findBySlugForUser(
        slug: String,
        currentUsername: String?,
        currentUserRole: Role?
    ): Post {
        val post = postRepository.findBySlug(slug)
            ?: throw NoSuchElementException("Post not found")
        
        // Published posts are visible to everyone
        if (post.status == PostStatus.PUBLISHED) {
            metricsService.recordPostView()
            return post
        }
        
        // Unpublished posts require authentication
        if (currentUsername == null) {
            throw NoSuchElementException("Post not found")
        }
        
        val currentUser = userRepository.findByUsername(Username(currentUsername))
            ?: throw NoSuchElementException("Post not found")
        
        // Allow if user is owner or admin
        if (post.isOwnedBy(currentUser.id) || currentUserRole == Role.ADMIN) {
            return post
        }
        
        throw NoSuchElementException("Post not found")
    }
    
    private fun generateUniqueSlug(title: String, excludePostId: ULID): String {
        val baseSlug = generateSlug(title)
        var slug = baseSlug
        var counter = 2
        
        while (true) {
            val existing = postRepository.findBySlug(slug)
            if (existing == null || existing.id == excludePostId) {
                return slug
            }
            slug = "$baseSlug-$counter"
            counter++
        }
    }
    
    // Existing private methods...
}
```

#### ScheduledPublishingService (New)

```kotlin
@Service
class ScheduledPublishingService(
    private val postRepository: PostRepository,
    private val metricsService: MetricsService,
    private val auditLogger: AuditLogger
) {
    private val logger = LoggerFactory.getLogger(ScheduledPublishingService::class.java)
    
    @Scheduled(fixedDelayString = "\${app.scheduled-publishing.interval:60000}")
    @CacheEvict(value = ["postBySlug", "postSummaries"], allEntries = true)
    @Transactional
    fun publishScheduledPosts() {
        logger.debug("Running scheduled post publishing job")
        val start = System.nanoTime()
        
        try {
            val now = OffsetDateTime.now()
            val postsToPublish = postRepository.findScheduledPostsReadyToPublish(now)
            
            logger.info("Found {} posts ready to publish", postsToPublish.size)
            
            postsToPublish.forEach { post ->
                try {
                    val published = post.withUpdatedFields(
                        status = PostStatus.PUBLISHED,
                        publishedAt = post.publishedAt ?: now
                    )
                    
                    postRepository.save(published)
                    
                    metricsService.recordPostAutoPublished()
                    auditLogger.logPostAutoPublished(
                        postId = post.id.toString(),
                        slug = post.slug.value,
                        scheduledTime = post.publishedAt.toString()
                    )
                    
                    logger.info("Auto-published post: {}", post.slug.value)
                } catch (e: Exception) {
                    logger.error("Failed to auto-publish post {}: {}", post.slug.value, e.message, e)
                }
            }
            
            metricsService.recordScheduledPublishingJobTime(
                Duration.ofNanos(System.nanoTime() - start)
            )
        } catch (e: Exception) {
            logger.error("Scheduled publishing job failed: {}", e.message, e)
        }
    }
}
```

### 3. Infrastructure Layer Changes

#### PostRepositoryImpl

```kotlin
@Repository
class PostRepositoryImpl(
    private val postRepositoryPostgresql: PostRepositoryPostgresql,
    // ... existing dependencies
) : PostRepository {
    // Existing methods...
    
    override fun findBySlug(slug: String): Post? {
        return postRepositoryPostgresql.findBySlug(slug)?.toDomain()
    }
    
    override fun findBySlugAndAuthorId(slug: String, authorId: ULID): Post? {
        return postRepositoryPostgresql.findBySlugAndAuthorId(slug, authorId.toString())?.toDomain()
    }
    
    override fun findScheduledPostsReadyToPublish(now: OffsetDateTime): List<Post> {
        return postRepositoryPostgresql.findByStatusAndPublishedAtLessThanEqual(
            PostStatus.SCHEDULED,
            now
        ).map { it.toDomain() }
    }
    
    override fun delete(post: Post) {
        val entity = postRepositoryPostgresql.findBySlug(post.slug.value)
            ?: throw NoSuchElementException("Post not found")
        postRepositoryPostgresql.delete(entity)
    }
}
```

#### PostRepositoryPostgresql (JPA Interface)

```kotlin
@Repository
interface PostRepositoryPostgresql : JpaRepository<PostEntity, String> {
    // Existing methods...
    fun findBySlugAndStatus(slug: String, status: PostStatus): PostEntity?
    
    // New methods
    fun findBySlug(slug: String): PostEntity?
    fun findBySlugAndAuthorId(slug: String, authorId: String): PostEntity?
    fun findByStatusAndPublishedAtLessThanEqual(status: PostStatus, publishedAt: OffsetDateTime): List<PostEntity>
}
```

#### PostsController

```kotlin
@RestController
@RequestMapping("/api/posts")
class PostsController(
    private val postService: PostService
) {
    // Existing methods...
    
    @PutMapping("/{slug}")
    @ResponseStatus(HttpStatus.OK)
    fun updatePost(
        @PathVariable slug: String,
        @RequestBody request: UpdatePostRequest,
        authentication: Authentication
    ): PostResponse {
        logger.info("Received request to update post: {}", slug)
        
        val userRole = authentication.authorities
            .map { it.authority }
            .firstOrNull { it.startsWith("ROLE_") }
            ?.removePrefix("ROLE_")
            ?.let { Role.valueOf(it) }
            ?: Role.USER
        
        val post = postService.updatePost(
            slug = slug,
            title = request.title?.value,
            content = request.content?.value,
            status = request.status,
            categoryName = request.categoryName?.value,
            tagNames = request.tagNames?.map { it.value },
            currentUsername = authentication.name,
            currentUserRole = userRole
        )
        
        return post.toResponse()
    }
    
    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePost(
        @PathVariable slug: String,
        authentication: Authentication
    ) {
        logger.info("Received request to delete post: {}", slug)
        
        val userRole = authentication.authorities
            .map { it.authority }
            .firstOrNull { it.startsWith("ROLE_") }
            ?.removePrefix("ROLE_")
            ?.let { Role.valueOf(it) }
            ?: Role.USER
        
        postService.deletePost(
            slug = slug,
            currentUsername = authentication.name,
            currentUserRole = userRole
        )
    }
    
    // Modify existing getPostBySlug to support unpublished posts for owners
    @GetMapping("/{slug}")
    @ResponseStatus(HttpStatus.OK)
    fun getPostBySlug(
        @PathVariable slug: String,
        authentication: Authentication?
    ): PostResponse {
        logger.info("Received request to get post by slug: {}", slug)
        
        val userRole = authentication?.authorities
            ?.map { it.authority }
            ?.firstOrNull { it.startsWith("ROLE_") }
            ?.removePrefix("ROLE_")
            ?.let { Role.valueOf(it) }
        
        val post = postService.findBySlugForUser(
            slug = slug,
            currentUsername = authentication?.name,
            currentUserRole = userRole
        )
        
        return post.toResponse()
    }
}
```

#### MetricsService

```kotlin
@Service
class MetricsService(private val meterRegistry: MeterRegistry) {
    // Existing metrics...
    
    private val postUpdates: Counter =
        Counter.builder("posts.updated")
            .description("Total number of posts updated")
            .register(meterRegistry)
    
    private val postDeletions: Counter =
        Counter.builder("posts.deleted")
            .description("Total number of posts deleted")
            .register(meterRegistry)
    
    private val postAutoPublished: Counter =
        Counter.builder("posts.auto_published")
            .description("Total number of posts auto-published")
            .register(meterRegistry)
    
    private val scheduledPublishingJobTimer: Timer =
        Timer.builder("posts.scheduled_publishing.time")
            .description("Time taken to run scheduled publishing job")
            .register(meterRegistry)
    
    fun recordPostUpdate() = postUpdates.increment()
    fun recordPostDeletion() = postDeletions.increment()
    fun recordPostAutoPublished() = postAutoPublished.increment()
    fun recordScheduledPublishingJobTime(duration: Duration) {
        scheduledPublishingJobTimer.record(duration)
    }
}
```

#### AuditLogger

```kotlin
@Component
class AuditLogger {
    // Existing methods...
    
    fun logPostUpdate(postId: String, username: String, oldSlug: String, newSlug: String) {
        logEvent(
            action = "POST_UPDATED",
            result = "SUCCESS",
            username = username,
            clientIp = "",
            userAgent = "",
            details = mapOf(
                "postId" to postId,
                "oldSlug" to oldSlug,
                "newSlug" to newSlug
            )
        )
    }
    
    fun logPostDeletion(postId: String, username: String, slug: String) {
        logEvent(
            action = "POST_DELETED",
            result = "SUCCESS",
            username = username,
            clientIp = "",
            userAgent = "",
            details = mapOf(
                "postId" to postId,
                "slug" to slug
            )
        )
    }
    
    fun logPostAutoPublished(postId: String, slug: String, scheduledTime: String) {
        logEvent(
            action = "POST_AUTO_PUBLISHED",
            result = "SUCCESS",
            username = "SYSTEM",
            clientIp = "",
            userAgent = "",
            details = mapOf(
                "postId" to postId,
                "slug" to slug,
                "scheduledTime" to scheduledTime
            )
        )
    }
}
```

## Data Models

### Request DTOs

```kotlin
data class UpdatePostRequest(
    val title: PostTitle?,
    val content: PostContent?,
    val status: PostStatus?,
    val categoryName: CategoryName?,
    val tagNames: List<TagName>?
)
```

All other data models remain unchanged. The Post entity already has all necessary fields.


## Correctness Properties

A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.

### Property 1: Authorization for Post Modifications

*For any* post and any authenticated user, when the user attempts to edit or delete the post, the operation should succeed if and only if the user is the post owner or has ADMIN role.

**Validates: Requirements 1.1, 2.1, 6.1, 6.2**

### Property 2: Slug Generation from Title

*For any* valid post title, when generating a slug, the result should be lowercase, contain only alphanumeric characters and hyphens, have spaces replaced with hyphens, have special characters removed, and satisfy the PostSlug value class constraints.

**Validates: Requirements 1.2, 7.1, 7.4**

### Property 3: Slug Conflict Resolution

*For any* post title that generates a slug conflicting with an existing post, the system should append a numeric suffix (starting with -2) to ensure uniqueness, and when checking conflicts, should exclude the post being updated.

**Validates: Requirements 1.3, 7.2**

### Property 4: UpdatedAt Timestamp on Modification

*For any* post that is successfully updated, the updatedAt timestamp should be set to a value greater than or equal to the time the update operation started.

**Validates: Requirements 1.4**

### Property 5: Field Updates Persistence

*For any* post and any valid update request specifying title, content, status, category, or tags, after the update operation, the post should reflect all the specified changes.

**Validates: Requirements 1.10**

### Property 6: Soft Delete Sets DeletedAt

*For any* post that is deleted, the deletedAt timestamp should be set to a non-null value, and the post should no longer appear in any standard queries.

**Validates: Requirements 2.2, 2.3**

### Property 7: Post Visibility Rules

*For any* post and any user request, the post should be visible if: (1) the post status is PUBLISHED, OR (2) the requesting user is authenticated and is the post owner, OR (3) the requesting user has ADMIN role. All other requests should be rejected.

**Validates: Requirements 3.1, 3.2, 3.3, 3.5**

### Property 8: Scheduled Post Query Correctness

*For any* given timestamp, querying for scheduled posts ready to publish should return all and only posts with status SCHEDULED and publishedAt less than or equal to that timestamp.

**Validates: Requirements 4.2**

### Property 9: Scheduled Post Publishing State Transition

*For any* scheduled post that is ready to publish, after the publishing operation, the post status should be PUBLISHED and publishedAt should be non-null.

**Validates: Requirements 4.3, 4.4**

### Property 10: Error Resilience in Batch Processing

*For any* list of scheduled posts to publish, if processing one post throws an exception, the system should continue processing the remaining posts in the list.

**Validates: Requirements 4.9**

## Error Handling

### Authorization Errors

- Use Spring Security's `AccessDeniedException` for authorization failures
- Return HTTP 403 Forbidden with generic message: "Access denied"
- Never reveal whether a post exists in error messages to prevent information disclosure

### Not Found Errors

- Use `NoSuchElementException` for missing posts
- Return HTTP 404 Not Found with generic message: "Post not found"
- Same message for both non-existent posts and unauthorized access to unpublished posts

### Validation Errors

- Value class constructors already throw `IllegalArgumentException` for invalid inputs
- Spring will convert these to HTTP 400 Bad Request automatically
- Slug conflicts during update should retry with suffixes, not fail

### Scheduled Publishing Errors

- Log errors at ERROR level with full stack trace
- Continue processing remaining posts (don't fail entire batch)
- Metrics should track failed auto-publish attempts

### Transaction Boundaries

- All service methods are `@Transactional` to ensure atomicity
- Cache eviction happens after successful transaction commit
- Rollback on any exception to maintain data consistency

## Testing Strategy

### Dual Testing Approach

This feature requires both unit tests and property-based tests for comprehensive coverage:

- **Unit tests**: Verify specific examples, edge cases, and integration points
- **Property tests**: Verify universal properties across all inputs

Together, these provide comprehensive coverage where unit tests catch concrete bugs and property tests verify general correctness.

### Property-Based Testing

We will use [Kotest Property Testing](https://kotest.io/docs/proptest/property-based-testing.html) for Kotlin. Each property test should:

- Run minimum 100 iterations to ensure comprehensive input coverage
- Use Kotest's built-in generators for primitives and custom generators for domain types
- Tag each test with a comment referencing the design property
- Tag format: `// Feature: post-management-enhancements, Property {number}: {property_text}`

Example property test structure:

```kotlin
class PostManagementPropertiesTest : FunSpec({
    test("Property 1: Authorization for Post Modifications") {
        // Feature: post-management-enhancements, Property 1: Authorization for Post Modifications
        checkAll(100, Arb.post(), Arb.user()) { post, user ->
            val canModify = postService.canUserModifyPost(post, user)
            val isOwner = post.author.id == user.id
            val isAdmin = user.role == Role.ADMIN
            
            canModify shouldBe (isOwner || isAdmin)
        }
    }
})
```

### Unit Testing Focus

Unit tests should cover:

- Specific examples of slug generation (e.g., "Hello World" → "hello-world")
- Edge cases like empty tag lists, null optional fields
- Integration between controller, service, and repository layers
- Error message content for security (generic messages)
- Scheduled job execution timing and configuration

### Integration Testing

Integration tests should verify:

- Cache invalidation on updates and deletes
- Metrics recording for all operations
- Audit logging with correct event details
- Database soft delete behavior via BaseEntity
- End-to-end flows: create → update → delete
- Scheduled publishing job execution

### Test Data Generators

Create Kotest Arb generators for:

- `Arb.post()`: Random posts with all statuses
- `Arb.user()`: Random users with different roles
- `Arb.postTitle()`: Valid post titles
- `Arb.postContent()`: Valid post content
- `Arb.category()`: Random categories
- `Arb.tag()`: Random tags

### Coverage Goals

- Minimum 80% line coverage for service layer
- 100% coverage of authorization logic
- All correctness properties implemented as property tests
- All edge cases covered by unit tests
