package br.dev.demoraes.abrolhos.domain.services

import br.dev.demoraes.abrolhos.domain.entities.Tag
import br.dev.demoraes.abrolhos.domain.entities.TagName
import br.dev.demoraes.abrolhos.domain.entities.TagSlug
import br.dev.demoraes.abrolhos.domain.repository.TagRepository
import br.dev.demoraes.abrolhos.domain.services.commands.CreateTagCommand
import br.dev.demoraes.abrolhos.domain.services.commands.UpdateTagCommand
import ulid.ULID
import java.time.OffsetDateTime

class TagService(
    private val tagRepository: TagRepository,
) {
    fun listTags(): List<Tag> = tagRepository.findAll()

    fun getBySlug(slug: String): Tag? = tagRepository.findBySlug(TagSlug(slug))

    fun create(command: CreateTagCommand): Tag {
        val name = TagName(command.name)
        val slug = generateSlug(command.name)

        // Check if tag with same name or slug already exists
        tagRepository.findByName(name)?.let {
            throw IllegalStateException("Tag with name ${command.name} already exists")
        }
        tagRepository.findBySlug(slug)?.let {
            throw IllegalStateException("Tag with slug ${slug.value} already exists")
        }

        val now = OffsetDateTime.now()
        val tag =
            Tag(
                id = ULID.nextULID(),
                name = name,
                slug = slug,
                posts = emptySet(),
                createdAt = now,
                updatedAt = now,
            )

        return tagRepository.save(tag)
    }

    fun update(command: UpdateTagCommand): Tag {
        val name = TagName(command.name)
        val slug = generateSlug(command.name)

        val tag =
            tagRepository.findById(ULID.parseULID(command.id))
                ?: throw NoSuchElementException("Tag with ID ${command.id} not found")

        // Check if updated name/slug would conflict with existing tags
        tagRepository.findByName(name)?.let {
            if (it.id != tag.id) {
                throw IllegalStateException("Tag with name ${command.name} already exists")
            }
        }
        tagRepository.findBySlug(slug)?.let {
            if (it.id != tag.id) {
                throw IllegalStateException("Tag with slug ${slug.value} already exists")
            }
        }

        val updatedTag =
            tag.copy(
                name = name,
                slug = slug,
                updatedAt = OffsetDateTime.now(),
            )

        return tagRepository.save(updatedTag)
    }

    fun delete(id: String) {
        val tag =
            tagRepository.findById(ULID.parseULID(id))
                ?: throw NoSuchElementException("Tag with ID $id not found")

        tagRepository.delete(tag)
    }

    private fun generateSlug(name: String): TagSlug {
        val slug =
            name.lowercase()
                .replace(Regex("[^a-z0-9\\s-]"), "")
                .replace(Regex("\\s+"), "-")
        return TagSlug(slug)
    }
}
