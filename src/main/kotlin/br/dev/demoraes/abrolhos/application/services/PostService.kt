package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.application.audit.AuditLogger
import br.dev.demoraes.abrolhos.domain.entities.Category
import br.dev.demoraes.abrolhos.domain.entities.CategoryName
import br.dev.demoraes.abrolhos.domain.entities.CategorySlug
import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.PostContent
import br.dev.demoraes.abrolhos.domain.entities.PostSlug
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostSummary
import br.dev.demoraes.abrolhos.domain.entities.PostTitle
import br.dev.demoraes.abrolhos.domain.entities.Role
import br.dev.demoraes.abrolhos.domain.entities.Tag
import br.dev.demoraes.abrolhos.domain.entities.TagName
import br.dev.demoraes.abrolhos.domain.entities.TagSlug
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.repository.CategoryRepository
import br.dev.demoraes.abrolhos.domain.repository.PostRepository
import br.dev.demoraes.abrolhos.domain.repository.TagRepository
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import br.dev.demoraes.abrolhos.infrastructure.monitoring.MetricsService
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ulid.ULID
import java.time.Duration
import java.time.OffsetDateTime

/**
 * Service for managing blog posts.
 *
 * This service handles the lifecycle of posts:
 * - Creation of new posts with proper slug generation.
 * - Retrieval of posts by slug (for public view and author view).
 * - Updating posts with authorization checks and slug conflict resolution.
 * - Soft-deleting posts with authorization checks.
 * - Searching and filtering posts (for lists/admin).
 *
 * It ensures referential integrity by finding or creating Categories and Tags as needed.
 */
@Service
@Transactional
class PostService(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
    private val metricsService: MetricsService,
    private val auditLogger: AuditLogger,
) {
    private val logger = LoggerFactory.getLogger(PostService::class.java)

    @CacheEvict(value = ["postBySlug", "postSummaries"], allEntries = true)
    fun createPost(
        title: String,
        content: String,
        status: PostStatus,
        categoryName: String,
        tagNames: List<String>,
        authorUsername: String
    ): Post {
        logger.debug("Creating post: title={}, author={}", title, authorUsername)
        val author =
            userRepository.findByUsername(Username(authorUsername))
                ?: throw NoSuchElementException("Author not found").also {
                    logger.error("Author not found: {}", authorUsername)
                }

        val slug = generateSlug(title)

        val category = findOrCreateCategory(categoryName)
        val tags = tagNames.map { findOrCreateTag(it) }.toSet()

        val post =
            Post(
                id = ULID.nextULID(),
                author = author,
                title = PostTitle(title),
                slug = PostSlug(slug),
                content = PostContent(content),
                status = status,
                category = category,
                tags = tags,
                publishedAt =
                if (status == PostStatus.PUBLISHED) {
                    OffsetDateTime.now()
                } else {
                    null
                },
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )

        return postRepository.save(post).also {
            logger.debug("Post saved with slug: {}", it.slug.value)
            metricsService.recordPostCreation()
        }
    }

    /** Returns a published post by slug. Uses cache. */
    @Cacheable(value = ["postBySlug"], key = "#slug")
    fun findPublishedBySlug(slug: String): Post {
        logger.debug("Finding published post by slug: {}", slug)
        return postRepository.findPublishedBySlug(slug)?.also { metricsService.recordPostView() }
            ?: throw NoSuchElementException("Post with slug '$slug' not found").also {
                logger.warn("Published post with slug '{}' not found", slug)
            }
    }

    /**
     * Returns a post by slug applying visibility rules:
     * - PUBLISHED posts are visible to everyone.
     * - DRAFT / SCHEDULED posts are visible only to the post owner or an ADMIN.
     * - Unauthenticated users see only published posts.
     *
     * Uses generic 404 messages to prevent information disclosure.
     */
    fun findBySlugForUser(
        slug: String,
        currentUsername: String?,
        currentUserRole: Role?,
    ): Post {
        val post = postRepository.findBySlug(slug) ?: throw NoSuchElementException("Post not found")

        if (post.status == PostStatus.PUBLISHED) {
            metricsService.recordPostView()
            return post
        }

        // Unpublished post — require authentication
        if (currentUsername == null) {
            throw NoSuchElementException("Post not found")
        }

        // ADMIN can see anything
        if (currentUserRole == Role.ADMIN) {
            return post
        }

        val currentUser =
            userRepository.findByUsername(Username(currentUsername))
                ?: throw NoSuchElementException("Post not found")

        if (post.isOwnedBy(currentUser.id)) {
            return post
        }

        throw NoSuchElementException("Post not found")
    }

    @CacheEvict(value = ["postBySlug", "postSummaries"], allEntries = true)
    fun updatePost(
        slug: String,
        title: String?,
        content: String?,
        status: PostStatus?,
        categoryName: String?,
        tagNames: List<String>?,
        currentUsername: String,
        currentUserRole: Role,
    ): Post {
        logger.debug("Updating post slug={} by user={}", slug, currentUsername)

        val post = postRepository.findBySlug(slug) ?: throw NoSuchElementException("Post not found")

        val currentUser =
            userRepository.findByUsername(Username(currentUsername))
                ?: throw NoSuchElementException("User not found")

        if (!post.isOwnedBy(currentUser.id) && currentUserRole != Role.ADMIN) {
            throw AccessDeniedException("Access denied")
        }

        // Regenerate slug only when the title actually changes
        val newSlug =
            if (title != null && title != post.title.value) {
                PostSlug(generateUniqueSlug(title, post.id))
            } else {
                null
            }

        val newCategory = categoryName?.let { findOrCreateCategory(it) }
        val newTags = tagNames?.map { findOrCreateTag(it) }?.toSet()

        val newPublishedAt =
            if (status == PostStatus.PUBLISHED && post.publishedAt == null) {
                OffsetDateTime.now()
            } else {
                null
            }

        val updatedPost =
            post.withUpdatedFields(
                title = title?.let { PostTitle(it) },
                slug = newSlug,
                content = content?.let { PostContent(it) },
                status = status,
                category = newCategory,
                tags = newTags,
                publishedAt = newPublishedAt,
            )

        val saved = postRepository.save(updatedPost)

        metricsService.recordPostUpdate()
        auditLogger.logPostUpdate(
            postId = post.id.toString(),
            username = currentUsername,
            oldSlug = post.slug.value,
            newSlug = saved.slug.value,
        )

        logger.info("Post updated: oldSlug={}, newSlug={}", post.slug.value, saved.slug.value)
        return saved
    }

    @CacheEvict(value = ["postBySlug", "postSummaries"], allEntries = true)
    fun deletePost(
        slug: String,
        currentUsername: String,
        currentUserRole: Role,
    ) {
        logger.debug("Deleting post slug={} by user={}", slug, currentUsername)

        val post = postRepository.findBySlug(slug) ?: throw NoSuchElementException("Post not found")

        val currentUser =
            userRepository.findByUsername(Username(currentUsername))
                ?: throw NoSuchElementException("User not found")

        if (!post.isOwnedBy(currentUser.id) && currentUserRole != Role.ADMIN) {
            throw AccessDeniedException("Access denied")
        }

        postRepository.delete(post)

        metricsService.recordPostDeletion()
        auditLogger.logPostDeletion(
            postId = post.id.toString(),
            username = currentUsername,
            slug = post.slug.value,
        )

        logger.info("Post soft-deleted: slug={}", slug)
    }

    @Cacheable(
        value = ["postSummaries"],
        condition = "#categoryName == null && #tagName == null && #pageable.pageNumber == 0"
    )
    fun searchPostSummaries(
        pageable: Pageable,
        categoryName: String?,
        tagName: String?,
        status: PostStatus
    ): Page<PostSummary> {
        logger.debug(
            "Searching for posts summary with pageable: {}, category: {}, tag: {}, status: {}",
            pageable,
            categoryName,
            tagName,
            status
        )
        val start = System.nanoTime()
        return postRepository.searchSummary(pageable, categoryName, tagName, status).also {
            metricsService.recordPostQueryTime(Duration.ofNanos(System.nanoTime() - start))
        }
    }

    fun searchPostSummariesByCursor(
        cursor: String?,
        size: Int,
        status: PostStatus
    ): br.dev.demoraes.abrolhos.domain.entities.CursorPage<PostSummary> {
        logger.debug(
            "Searching for posts summary by cursor: {}, size: {}, status: {}",
            cursor,
            size,
            status
        )
        return postRepository.searchSummaryByCursor(cursor, size, status)
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Generates a slug from [title], ensuring uniqueness by appending -2, -3, … when a conflict
     * exists. The post identified by [excludePostId] is excluded from conflict checks so that
     * updating a post to its own slug does not trigger a false conflict.
     */
    internal fun generateUniqueSlug(title: String, excludePostId: ULID): String {
        val baseSlug = generateSlug(title)
        var candidate = baseSlug
        var counter = 2

        while (true) {
            val existing = postRepository.findBySlug(candidate)
            if (existing == null || existing.id == excludePostId) {
                return candidate
            }
            candidate = "$baseSlug-$counter"
            counter++
        }
    }

    private fun findOrCreateCategory(name: String): Category {
        val categoryName = CategoryName(name)
        return categoryRepository.findByName(categoryName)
            ?: run {
                logger.info("Category '{}' not found, creating new one", name)
                val slug = generateSlug(name)
                categoryRepository.save(
                    Category(
                        id = ULID.nextULID(),
                        name = categoryName,
                        slug = CategorySlug(slug),
                        posts = emptySet(),
                        createdAt = OffsetDateTime.now(),
                        updatedAt = OffsetDateTime.now()
                    )
                )
            }
    }

    private fun findOrCreateTag(name: String): Tag {
        val tagName = TagName(name)
        val slug = generateSlug(name)
        return tagRepository.findBySlug(TagSlug(slug))
            ?: run {
                logger.info("Tag '{}' not found, creating new one", name)
                tagRepository.save(
                    Tag(
                        id = ULID.nextULID(),
                        name = tagName,
                        slug = TagSlug(slug),
                        posts = emptySet(),
                        createdAt = OffsetDateTime.now(),
                        updatedAt = OffsetDateTime.now()
                    )
                )
            }
    }

    private fun generateSlug(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), "-")
            .trim('-')
    }
}
