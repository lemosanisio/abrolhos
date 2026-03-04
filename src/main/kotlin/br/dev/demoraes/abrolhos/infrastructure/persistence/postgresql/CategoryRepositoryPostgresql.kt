package br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql

import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.CategoryEntity
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Spring Data JPA repository for CategoryEntity.
 *
 * Provides standard CRUD operations and custom JPQL queries for Category persistence.
 */
interface CategoryRepositoryPostgresql : JpaRepository<CategoryEntity, String> {
    fun findByName(name: String): CategoryEntity?

    fun findByNameIn(names: Set<String>): Set<CategoryEntity>

    fun findBySlug(slug: String): CategoryEntity?
}
