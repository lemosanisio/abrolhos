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
    private val categoryRepositoryPostgresql: CategoryRepositoryPostgresql,
) : CategoryRepository {
    override fun findByName(name: CategoryName): Category? =
        categoryRepositoryPostgresql.findByName(name.value)?.toDomain()

    override fun save(category: Category): Category =
        categoryRepositoryPostgresql
            .save(
                CategoryEntity(
                    name = category.name.value,
                    slug = category.slug.value,
                )
                    .apply {
                        id = category.id.toString()
                        createdAt = category.createdAt
                        updatedAt = category.updatedAt
                    },
            )
            .toDomain()

    private fun CategoryEntity.toDomain(): Category {
        val created =
            this.createdAt
                ?: error(
                    "CategoryEntity with id ${this.id} is missing a createdAt timestamp.",
                )
        val updated =
            this.updatedAt
                ?: error(
                    "CategoryEntity with id ${this.id} is missing an updatedAt timestamp.",
                )

        return Category(
            id = ULID.parseULID(this.id),
            name = CategoryName(this.name),
            slug = CategorySlug(this.slug),
            posts = emptySet(),
            createdAt = created,
            updatedAt = updated,
        )
    }
}
