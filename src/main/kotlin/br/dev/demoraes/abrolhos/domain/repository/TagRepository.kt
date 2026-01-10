package br.dev.demoraes.abrolhos.domain.repository

import br.dev.demoraes.abrolhos.domain.entities.Tag
import br.dev.demoraes.abrolhos.domain.entities.TagName

interface TagRepository {
    fun findByName(name: TagName): Tag?

    fun save(tag: Tag): Tag
}
