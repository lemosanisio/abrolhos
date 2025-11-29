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
    private val tagRepositoryPostgresql: TagRepositoryPostgresql,
) : TagRepository {
    override fun findByName(name: TagName): Tag? = tagRepositoryPostgresql.findByName(name.value)?.toDomain()

    override fun findByNameIn(names: Set<TagName>): Set<Tag?> =
        tagRepositoryPostgresql
            .findByNameIn(names.map { it.value }.toSet())
            .map { it.toDomain() }
            .toSet()

    override fun findBySlug(slug: TagSlug): Tag? = tagRepositoryPostgresql.findBySlug(slug.value)?.toDomain()

    override fun findAll(): List<Tag> = tagRepositoryPostgresql.findAll().map { it.toDomain() }

    override fun findById(id: ULID): Tag? =
        tagRepositoryPostgresql.findById(id.toString()).map { it.toDomain() }.orElse(null)

    override fun save(tag: Tag): Tag =
        tagRepositoryPostgresql
            .save(
                TagEntity(
                    name = tag.name.value,
                    slug = tag.slug.value,
                )
                    .apply {
                        id = tag.id.toString()
                        createdAt = tag.createdAt
                        updatedAt = tag.updatedAt
                    },
            )
            .toDomain()

    override fun delete(tag: Tag) {
        tagRepositoryPostgresql.deleteById(tag.id.toString())
    }

    private fun TagEntity.toDomain(): Tag {
        val created =
            this.createdAt
                ?: error(
                    "TagEntity with id ${this.id} is missing a createdAt timestamp.",
                )
        val updated =
            this.updatedAt
                ?: error(
                    "TagEntity with id ${this.id} is missing an updatedAt timestamp.",
                )

        return Tag(
            id = ULID.parseULID(this.id),
            name = TagName(this.name),
            slug = TagSlug(this.slug),
            posts = emptySet(),
            createdAt = created,
            updatedAt = updated,
        )
    }
}
