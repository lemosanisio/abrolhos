package br.dev.demoraes.abrolhos.domain.repository

import br.dev.demoraes.abrolhos.domain.entities.Tag
import br.dev.demoraes.abrolhos.domain.entities.TagName
import br.dev.demoraes.abrolhos.domain.entities.TagSlug
import ulid.ULID

interface TagRepository {
    fun findByName(name: TagName): Tag?

    fun findByNameIn(names: Set<TagName>): Set<Tag?>

    fun findBySlug(slug: TagSlug): Tag?

    fun findAll(): List<Tag>

    fun findById(id: ULID): Tag?

    fun save(tag: Tag): Tag

    fun delete(tag: Tag)
}
