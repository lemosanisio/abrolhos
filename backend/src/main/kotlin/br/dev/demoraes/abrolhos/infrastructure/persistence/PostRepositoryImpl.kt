package br.dev.demoraes.abrolhos.infrastructure.persistence

import br.dev.demoraes.abrolhos.domain.entities.Category
import br.dev.demoraes.abrolhos.domain.entities.CategoryName
import br.dev.demoraes.abrolhos.domain.entities.CategorySlug
import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.PostContent
import br.dev.demoraes.abrolhos.domain.entities.PostSlug
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostTitle
import br.dev.demoraes.abrolhos.domain.entities.Tag
import br.dev.demoraes.abrolhos.domain.entities.TagName
import br.dev.demoraes.abrolhos.domain.entities.TagSlug
import br.dev.demoraes.abrolhos.domain.repository.PostRepository
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.CategoryEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.PostEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.TagEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.UserEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql.CategoryRepositoryPostgresql
import br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql.PostRepositoryPostgresql
import br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql.TagRepositoryPostgresql
import br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql.UserRepositoryPostgresql
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import ulid.ULID

@Repository
class PostRepositoryImpl(
        private val postRepositoryPostgresql: PostRepositoryPostgresql,
        private val categoryJpa: CategoryRepositoryPostgresql,
        private val tagJpa: TagRepositoryPostgresql,
        private val userJpa: UserRepositoryPostgresql,
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

        override fun findPublished(): Set<Post> {
                return postRepositoryPostgresql
                        .findByStatus(PostStatus.PUBLISHED)
                        .map { it.toDomain() }
                        .toSet()
        }

        override fun findPublishedBySlug(slug: String): Post? {
                return postRepositoryPostgresql
                        .findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                        ?.toDomain()
        }

        override fun findPublishedByCategory(categorySlug: String): Set<Post> {
                return postRepositoryPostgresql
                        .findByCategorySlugAndStatus(categorySlug, PostStatus.PUBLISHED)
                        .map { it.toDomain() }
                        .toSet()
        }

        override fun findPublishedByTag(tagSlug: String): Set<Post> {
                return postRepositoryPostgresql
                        .findByTagsSlugAndStatus(tagSlug, PostStatus.PUBLISHED)
                        .map { it.toDomain() }
                        .toSet()
        }

        private fun Post.toEntity(): PostEntity {
                val authorEntity: UserEntity =
                        userJpa.findByIdOrNull(this.author.id.toString())
                                ?: throw NoSuchElementException(
                                        "Author with id ${this.author.id} not found"
                                )

                val categoryEntity: CategoryEntity? =
                        categoryJpa.findBySlug(this.category.slug.value)

                val tagEntities: MutableSet<TagEntity> =
                        tagJpa.findByNameIn(this.tags.map { it.name.value }.toSet()).toMutableSet()

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
                        .apply {
                                id = this@toEntity.id.toString()
                                createdAt = this@toEntity.createdAt
                                updatedAt = this@toEntity.updatedAt
                        }
        }
}

private fun CategoryEntity.toDomain(): Category {
        val created =
                this.createdAt
                        ?: error(
                                "CategoryEntity with id ${this.id} is missing a createdAt timestamp."
                        )
        val updated =
                this.updatedAt
                        ?: error(
                                "CategoryEntity with id ${this.id} is missing an updatedAt timestamp."
                        )

        return Category(
                id = ULID.parseULID(this.id),
                name = CategoryName(this.name),
                slug = CategorySlug(this.slug),
                posts = emptySet(),
                createdAt = created,
                updatedAt = updated,
        )
}

private fun TagEntity.toDomain(): Tag {
        val created =
                this.createdAt
                        ?: error("TagEntity with id ${this.id} is missing a createdAt timestamp.")
        val updated =
                this.updatedAt
                        ?: error("TagEntity with id ${this.id} is missing an updatedAt timestamp.")

        return Tag(
                id = ULID.parseULID(this.id),
                name = TagName(this.name),
                slug = TagSlug(this.slug),
                posts = emptySet(),
                createdAt = created,
                updatedAt = updated,
        )
}

fun PostEntity.toDomain(): Post {
        val createdAt =
                this.createdAt
                        ?: error(
                                "PostEntity with id ${this.id} is missing a createdAt timestamp. " +
                                        "This should not happen for a persisted entity.",
                        )

        val updatedAt =
                this.updatedAt
                        ?: error(
                                "PostEntity with id ${this.id} is missing an updatedAt timestamp. " +
                                        "This should not happen for a persisted entity.",
                        )

        val category =
                this.category?.toDomain()
                        ?: error(
                                "PostEntity with id ${this.id} is missing category information. " +
                                        "This should not happen for a persisted entity.",
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
                updatedAt = updatedAt,
        )
}
