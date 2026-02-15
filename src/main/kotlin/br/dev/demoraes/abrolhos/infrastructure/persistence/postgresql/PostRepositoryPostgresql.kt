package br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql

import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostSummary
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.PostEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * Spring Data JPA repository for PostEntity.
 *
 * Provides complex queries for searching, filtering, and projecting post data.
 */
@Repository
interface PostRepositoryPostgresql :
    JpaRepository<PostEntity, String>, JpaSpecificationExecutor<PostEntity> {
    fun findBySlug(slug: String): PostEntity?

    fun findByStatus(status: PostStatus, pageable: Pageable): Page<PostEntity>

    fun findBySlugAndStatus(
        slug: String,
        status: PostStatus,
    ): PostEntity?

    fun findByCategorySlugAndStatus(
        slug: String,
        status: PostStatus,
    ): List<PostEntity>

    fun findByTagsSlugAndStatus(
        slug: String,
        status: PostStatus,
    ): List<PostEntity>

    @Query(
        """
        SELECT
            p.id AS id,
            p.author.username AS authorUsername,
            p.title AS title,
            p.slug AS slug,
            p.category.name AS categoryName,
            p.publishedAt AS publishedAt,
            SUBSTRING(p.content, 1, 500) AS shortContent
        FROM PostEntity p
        WHERE p.status = :status
          AND (:categoryName IS NULL OR p.category.name = :categoryName)
          AND (:tagName IS NULL OR EXISTS (SELECT t FROM p.tags t WHERE t.name = :tagName))
    """,
    )
    fun searchSummary(
        status: PostStatus,
        categoryName: String?,
        tagName: String?,
        pageable: Pageable,
    ): Page<PostSummary>

    @Query(value = "SELECT * FROM posts", nativeQuery = true)
    fun findAllIncludingDeleted(): List<PostEntity>?

    @Query(value = "SELECT * FROM posts WHERE deleted_at IS NOT NULL", nativeQuery = true)
    fun findOnlyDeleted(): List<PostEntity>?
}
