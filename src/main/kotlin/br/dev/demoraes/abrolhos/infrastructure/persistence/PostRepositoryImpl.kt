package br.dev.demoraes.abrolhos.infrastructure.persistence

import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.PostContent
import br.dev.demoraes.abrolhos.domain.entities.PostSlug
import br.dev.demoraes.abrolhos.domain.entities.PostTitle
import br.dev.demoraes.abrolhos.domain.repository.PostRepository
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.PostEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql.PostRepositoryPostgresql
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import ulid.ULID

@Repository
class PostRepositoryImpl(
    private val postRepositoryPostgresql: PostRepositoryPostgresql,
    private val categoryRepository: CategoryRepositoryImpl,
    private val tagRepository: TagRepositoryImpl
) : PostRepository {
    override fun save(post: Post): Post {
        return postRepositoryPostgresql.save(post.toEntity()).toDomain()
    }

    override fun findById(postId: ULID): Post? {
        return postRepositoryPostgresql.findByIdOrNull(postId.toString())?.toDomain()
    }

    override fun findAll(): Set<Post> {
        return postRepositoryPostgresql.findAll().map { it.toDomain() }.toSet()
    }

    private fun Post.toEntity(): PostEntity {
        val authorEntity = this.author.toEntity()
        val categoryEntity = this.category.let { category ->
            categoryRepository.findByName(category.name)?.toEntity()
        }
        val tagEntities = tagRepository.findByNameIn(this.tags.map { it.name }.toSet())
            .map { it.toEntity() }
            .toMutableSet()

        return PostEntity(
            title = this.title.value,
            slug = this.slug.value,
            content = this.content.value,
            status = this.status,
            publishedAt = this.publishedAt,
            author = authorEntity,
            category = categoryEntity,
            tags = tagEntities,
        )
    }
}

fun PostEntity.toDomain(): Post {
    val createdAt = this.createdAt
        ?: throw IllegalStateException(
            "PostEntity with id ${this.id} is missing a createdAt timestamp. " +
                "This should not happen for a persisted entity."
        )

    val updatedAt = this.updatedAt
        ?: throw IllegalStateException(
            "PostEntity with id ${this.id} is missing an updatedAt timestamp. " +
                "This should not happen for a persisted entity."
        )

    val category = this.category?.toDomain()
        ?: throw IllegalStateException(
            "PostEntity with id ${this.id} is missing category information. " +
                "This should not happen for a persisted entity."
        )

    return Post(
        id = ULID.parseULID(this.id),
        title = PostTitle(this.title),
        slug = PostSlug(this.slug),
        content = PostContent(this.content),
        status = this.status,
        publishedAt = this.publishedAt,
        author = this.author.toDomain(),
        category = category,
        tags = this.tags.map { it.toDomain() }.toSet(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
