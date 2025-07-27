package br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql

import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.CategoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface CategoryRepository : JpaRepository<CategoryEntity, String> {
    fun findBySlug(slug: String): CategoryEntity?
    fun findByNameIn(names: Collection<String>): Set<CategoryEntity>
}