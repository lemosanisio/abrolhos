package br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql

import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.CategoryEntity
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepositoryPostgresql : JpaRepository<CategoryEntity, String> {
    fun findByName(name: String): CategoryEntity?

    fun findByNameIn(names: Set<String>): Set<CategoryEntity>

    fun findBySlug(slug: String): CategoryEntity?
}
