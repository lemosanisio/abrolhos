package br.dev.demoraes.abrolhos.domain.repository

import br.dev.demoraes.abrolhos.domain.entities.Post
import ulid.ULID

interface PostRepository {
    fun save(post: Post): Post
    fun findById(postId: ULID): Post?
    fun findAll(): Set<Post?>
}
