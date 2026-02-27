package br.dev.demoraes.abrolhos.infrastructure.persistence

import br.dev.demoraes.abrolhos.domain.entities.Category
import br.dev.demoraes.abrolhos.domain.entities.CategoryName
import br.dev.demoraes.abrolhos.domain.entities.CategorySlug
import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.PostContent
import br.dev.demoraes.abrolhos.domain.entities.PostSlug
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostSummary
import br.dev.demoraes.abrolhos.domain.entities.PostTitle
import br.dev.demoraes.abrolhos.domain.entities.Tag
import br.dev.demoraes.abrolhos.domain.entities.TagName
import br.dev.demoraes.abrolhos.domain.entities.TagSlug
import br.dev.demoraes.abrolhos.domain.repository.PostRepository
import br.dev.demoraes.abrolhos.infrastructure.cache.dto.PostSummaryDto
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.CategoryEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.PostEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.TagEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.UserEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql.CategoryRepositoryPostgresql
import br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql.PostRepositoryPostgresql
import br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql.TagRepositoryPostgresql
import br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql.UserRepositoryPostgresql
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import ulid.ULID

/**
 * Persistence implementation for Post repository.
 *
 * Bridges the Domain layer (PostRepository interface) and the Infrastructure layer (JPA/Hibernate).
 * Handles the mapping between Domain entities (Post) and Persistence entities (PostEntity).
 */
@Repository
class PostRepositoryImpl(
        private val postRepositoryPostgresql: PostRepositoryPostgresql,
        private val categoryJpa: CategoryRepositoryPostgresql,
        private val tagJpa: TagRepositoryPostgresql,
        private val userJpa: UserRepositoryPostgresql,
) : PostRepository {
        private val logger = LoggerFactory.getLogger(PostRepositoryImpl::class.java)

        override fun save(post: Post): Post {
                logger.debug("Saving post: {}", post.id)
                return postRepositoryPostgresql.save(post.toEntity()).toDomain()
        }

        override fun findPublishedBySlug(slug: String): Post? {
                return postRepositoryPostgresql
                        .findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                        ?.toDomain()
        }

        override fun searchSummary(
                pageable: Pageable,
                categoryName: String?,
                tagName: String?,
                status: PostStatus,
        ): Page<PostSummary> {
                val page: Page<PostSummary> =
                        postRepositoryPostgresql.searchSummary(
                                        status,
                                        categoryName,
                                        tagName,
                                        pageable
                                )
                                .map { projection ->
                                        PostSummaryDto(
                                                id = projection.id,
                                                authorUsername = projection.authorUsername,
                                                title = projection.title,
                                                slug = projection.slug,
                                                categoryName = projection.categoryName,
                                                shortContent = projection.shortContent,
                                                publishedAt = projection.publishedAt,
                                        )
                                } as
                                Page<PostSummary>
                return br.dev.demoraes.abrolhos.infrastructure.cache.dto.SerializablePage<
                        PostSummary>(page)
        }

        private fun Post.toEntity(): PostEntity {
                val authorEntity: UserEntity =
                        userJpa.findByIdOrNull(this.author.id.toString())
                                ?: throw NoSuchElementException(
                                        "Author with id ${this.author.id} not found",
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
                                "CategoryEntity with id ${this.id} is missing a createdAt timestamp.",
                        )
        val updated =
                this.updatedAt
                        ?: error(
                                "CategoryEntity with id ${this.id} is missing an updatedAt timestamp.",
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
        val (createdAt, updatedAt, category) = getRequiredFields()

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

private fun PostEntity.getRequiredFields():
        Triple<java.time.OffsetDateTime, java.time.OffsetDateTime, Category> {
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
        return Triple(createdAt, updatedAt, category)
}
