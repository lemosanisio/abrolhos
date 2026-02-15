package br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql

import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.TagEntity
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Spring Data JPA repository for TagEntity.
 *
 * Provides standard CRUD operations and custom JPQL queries for Tag persistence.
 */
interface TagRepositoryPostgresql : JpaRepository<TagEntity, String> {
    fun findByName(name: String): TagEntity?

    fun findByNameIn(names: Set<String>): Set<TagEntity>

    fun findBySlug(slug: String): TagEntity?
}
