package br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql

import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.PostEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PostRepositoryPostgresql : JpaRepository<PostEntity, String>, JpaSpecificationExecutor<PostEntity> {
    fun findBySlug(slug: String): PostEntity?

    fun findByStatus(status: PostStatus): List<PostEntity>

    fun findBySlugAndStatus(
        slug: String,
        status: PostStatus,
    ): PostEntity?

    @Query(value = "SELECT * FROM posts", nativeQuery = true)
    fun findAllIncludingDeleted(): List<PostEntity>?

    @Query(value = "SELECT * FROM posts WHERE deleted_at IS NOT NULL", nativeQuery = true)
    fun findOnlyDeleted(): List<PostEntity>?
}
