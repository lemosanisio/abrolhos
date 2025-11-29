package br.dev.demoraes.abrolhos.domain.entities

import br.dev.demoraes.abrolhos.application.web.dto.response.PostResponse
import ulid.ULID
import java.time.OffsetDateTime

data class Post(
    val id: ULID,
    val author: User,
    val title: PostTitle,
    val slug: PostSlug,
    val content: PostContent,
    val status: PostStatus,
    val publishedAt: OffsetDateTime?,
    val category: Category,
    val tags: Set<Tag>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    companion object {
        /**
         * Factory method to create a new Post in a DRAFT state.
         * It generates a new ID and sets the initial timestamps.
         */
        fun create(
            author: User,
            title: PostTitle,
            slug: PostSlug,
            content: PostContent,
            category: Category,
            tags: Set<Tag>,
        ): Post {
            return Post(
                id = ULID.nextULID(),
                author = author,
                title = title,
                slug = slug,
                content = content,
                status = PostStatus.DRAFT,
                category = category,
                tags = tags,
                createdAt = OffsetDateTime.now(), // Set initial timestamps
                updatedAt = OffsetDateTime.now(),
                publishedAt = null,
            )
        }
    }
}

enum class PostStatus {
    DRAFT,
    PUBLISHED,
    SCHEDULED,
    ARCHIVED,
}

// TODO(Should finish defining those value objects here; at least length)
@JvmInline
value class PostTitle(val value: String) {
    init {
        require(value.isNotBlank()) { "Post title cannot be blank" }
    }
}

@JvmInline
value class PostSlug(val value: String) {
    init {
        require(value.isNotBlank()) { "Post slug cannot be blank" }
    }
}

@JvmInline
value class PostContent(val value: String) {
    init {
        require(value.isNotBlank()) { "Post content cannot be blank" }
    }
}

fun Post.toResponse(): PostResponse =
    PostResponse(
        title = this.title.value,
        slug = this.slug.value,
        content = this.content.value,
        status = this.status,
        authorUsername = this.author.username.value,
        categoryName = this.category.name.value,
        tagNames = this.tags.map { it.name.value }.toSet(),
        publishedAt = this.publishedAt,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )

// Mapping to generated OpenAPI DTO
fun Post.toGeneratedResponse(): br.dev.demoraes.abrolhos.application.dto.PostResponse =
    br.dev.demoraes.abrolhos.application.dto.PostResponse(
        title = this.title.value,
        slug = this.slug.value,
        content = this.content.value,
        status = br.dev.demoraes.abrolhos.application.dto.PostResponse.Status.valueOf(this.status.name),
        authorUsername = this.author.username.value,
        categoryName = this.category.name.value,
        tagNames = this.tags.map { it.name.value },
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        publishedAt = this.publishedAt,
    )
