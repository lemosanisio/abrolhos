package br.dev.demoraes.abrolhos.domain.repository

import br.dev.demoraes.abrolhos.domain.entities.Post
import ulid.ULID

interface PostRepository {
    fun save(post: Post): Post

    fun findById(postId: ULID): Post?

    fun findAll(): Set<Post>

    fun findPublished(): Set<Post>

    fun findPublishedBySlug(slug: String): Post?

    fun findPublishedByCategory(categorySlug: String): Set<Post>

    fun findPublishedByTag(tagSlug: String): Set<Post>
}
