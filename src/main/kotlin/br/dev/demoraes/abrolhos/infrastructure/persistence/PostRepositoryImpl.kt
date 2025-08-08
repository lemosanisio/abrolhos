package br.dev.demoraes.abrolhos.infrastructure.persistence

import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.repository.PostRepository
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.CategoryEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.PostEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.TagEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.UserEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql.PostRepositoryPostgresql
import org.springframework.stereotype.Repository
import ulid.ULID

@Repository
class PostRepositoryImpl(
    private val postRepositoryPostgresql: PostRepositoryPostgresql
) : PostRepository {
    override fun save(post: Post): Post {
        return postRepositoryPostgresql.save<PostEntity>(post.toEntity()).toDomain()
    }

    override fun findById(postId: ULID): Post? {
        TODO("Not yet implemented")
    }

    override fun findAll(): Set<Post?> {
        TODO("Not yet implemented")
    }
}

// Dont remember how to do that without causing a stack overflow
fun Post.toEntity(): PostEntity {
    val entity = PostEntity(
        id = this.id,
        author = UserEntity(id = this.author.id),
        title = this.title.value,
        slug = this.slug.value,
        content = this.content.value,
        category = CategoryEntity,
        tag = ,
        status = this.status,
        publishedAt = this.publishedAt,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    return entity
}
