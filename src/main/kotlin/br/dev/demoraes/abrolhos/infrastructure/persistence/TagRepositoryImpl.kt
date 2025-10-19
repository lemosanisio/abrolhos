package br.dev.demoraes.abrolhos.infrastructure.persistence

import br.dev.demoraes.abrolhos.domain.entities.Tag
import br.dev.demoraes.abrolhos.domain.entities.TagName
import br.dev.demoraes.abrolhos.domain.entities.TagSlug
import br.dev.demoraes.abrolhos.domain.repository.TagRepository
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.TagEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql.TagRepositoryPostgresql
import org.springframework.stereotype.Repository
import ulid.ULID

@Repository
class TagRepositoryImpl(
    private val tagRepositoryPostgresql: TagRepositoryPostgresql
) : TagRepository {
    override fun findByName(name: TagName): Tag? {
        return tagRepositoryPostgresql.findByNameIn(setOf(name.value)).firstOrNull()?.toDomain()
    }

    override fun findByNameIn(names: Set<TagName>): Set<Tag> {
        val nameValues = names.map { it.value }.toSet()
        return tagRepositoryPostgresql.findByNameIn(nameValues).map { it.toDomain() }.toSet()
    }
}

fun Tag.toEntity() = TagEntity(
    name = this.name.value,
    slug = this.slug.value
)

fun TagEntity.toDomain(): Tag {
    val createdAt = this.createdAt
        ?: throw IllegalStateException(
            "TagEntity with id ${this.id} is missing a createdAt timestamp. " +
                "This should not happen for a persisted entity."
        )

    val updatedAt = this.updatedAt
        ?: throw IllegalStateException(
            "TagEntity with id ${this.id} is missing an updatedAt timestamp. " +
                "This should not happen for a persisted entity."
        )

    return Tag(
        id = ULID.parseULID(this.id),
        name = TagName(this.name),
        slug = TagSlug(this.slug),
        posts = emptySet(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
