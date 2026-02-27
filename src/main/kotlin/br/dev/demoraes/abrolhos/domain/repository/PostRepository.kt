package br.dev.demoraes.abrolhos.domain.repository

import br.dev.demoraes.abrolhos.domain.entities.CursorPage
import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostSummary
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Repository interface for Post entity persistence.
 *
 * Provides methods for CRUD operations and specialized queries like finding published posts by slug
 * and searching/filtering summaries.
 */
interface PostRepository {
    fun save(post: Post): Post

    fun findPublishedBySlug(slug: String): Post?

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
