package br.dev.demoraes.abrolhos.infrastructure.persistence

import br.dev.demoraes.abrolhos.domain.entities.Category
import br.dev.demoraes.abrolhos.domain.entities.CategoryName
import br.dev.demoraes.abrolhos.domain.entities.CategorySlug
import br.dev.demoraes.abrolhos.domain.repository.CategoryRepository
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.CategoryEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql.CategoryRepositoryPostgresql
import org.springframework.stereotype.Repository
import ulid.ULID

@Repository
class CategoryRepositoryImpl(
    private val categoryRepositoryPostgresql: CategoryRepositoryPostgresql
) : CategoryRepository {
    override fun findByName(name: CategoryName): Category? {
        return categoryRepositoryPostgresql.findByNameIn(setOf(name.value)).firstOrNull()?.toDomain()
    }

    override fun findByNameIn(names: Set<CategoryName>): Set<Category> {
        return categoryRepositoryPostgresql.findByNameIn(names.map { it.value }.toSet()).map { it.toDomain() }.toSet()
    }
}

fun Category.toEntity() = CategoryEntity(
    name = this.name.value,
    slug = this.slug.value
)

fun CategoryEntity.toDomain(): Category {
    val createdAt = this.createdAt
        ?: throw IllegalStateException(
            "CategoryEntity with id ${this.id} is missing a createdAt timestamp. " +
                "This should not happen for a persisted entity."
        )

    val updatedAt = this.updatedAt
        ?: throw IllegalStateException(
            "CategoryEntity with id ${this.id} is missing an updatedAt timestamp. " +
                "This should not happen for a persisted entity."
        )

    return Category(
        id = ULID.parseULID(this.id),
        name = CategoryName(this.name),
        slug = CategorySlug(this.slug),
        posts = emptySet(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
