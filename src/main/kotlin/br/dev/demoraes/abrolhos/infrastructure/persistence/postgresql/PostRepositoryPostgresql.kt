package br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql

import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.PostEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository

@Repository
interface PostRepository : JpaRepository<PostEntity, String>, JpaSpecificationExecutor<PostEntity> {
    fun findBySlug(slug: String): PostEntity?
}
