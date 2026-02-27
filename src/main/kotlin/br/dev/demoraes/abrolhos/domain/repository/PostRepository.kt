package br.dev.demoraes.abrolhos.domain.repository

import br.dev.demoraes.abrolhos.domain.entities.CursorPage
import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostSummary
import java.time.OffsetDateTime
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import ulid.ULID

/**
 * Repository interface for Post entity persistence.
 *
 * Provides methods for CRUD operations and specialized queries like finding published posts by slug
 * and searching/filtering summaries.
 */
interface PostRepository {
    fun save(post: Post): Post

    fun findPublishedBySlug(slug: String): Post?

    /** Finds a post by slug regardless of its status. */
    fun findBySlug(slug: String): Post?

    /** Finds a post by slug that belongs to the given author, regardless of status. */
    fun findBySlugAndAuthorId(slug: String, authorId: ULID): Post?

    /** Returns all posts with status SCHEDULED whose publishedAt is on or before [now]. */
    fun findScheduledPostsReadyToPublish(now: OffsetDateTime): List<Post>

    /** Soft-deletes a post via the BaseEntity @SQLDelete mechanism. */
    fun delete(post: Post)

    fun searchSummary(
            pageable: Pageable,
            categoryName: String?,
            tagName: String?,
            status: PostStatus
    ): Page<PostSummary>

    fun searchSummaryByCursor(
            cursor: String?,
            size: Int,
            status: PostStatus
    ): CursorPage<PostSummary>
}
