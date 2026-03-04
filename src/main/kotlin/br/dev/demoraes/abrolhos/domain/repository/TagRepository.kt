package br.dev.demoraes.abrolhos.domain.repository

import br.dev.demoraes.abrolhos.domain.entities.Tag
import br.dev.demoraes.abrolhos.domain.entities.TagName
import br.dev.demoraes.abrolhos.domain.entities.TagSlug

/**
 * Repository interface for Tag entity persistence.
 *
 * Handles storage and retrieval of blog tags.
 */
interface TagRepository {
    fun findByName(name: TagName): Tag?

    fun findBySlug(slug: TagSlug): Tag?

    fun save(tag: Tag): Tag
}
