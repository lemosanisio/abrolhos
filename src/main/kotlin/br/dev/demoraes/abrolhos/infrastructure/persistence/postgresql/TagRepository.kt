package br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql

import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.TagEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface TagRepository : JpaRepository<TagEntity, String> {
    fun findBySlug(slug: String): TagEntity?
    fun findByNameIn(names: Collection<String>): Set<TagEntity>
}