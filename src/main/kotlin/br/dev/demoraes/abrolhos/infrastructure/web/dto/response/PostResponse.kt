package br.dev.demoraes.abrolhos.infrastructure.web.dto.response

import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import java.time.OffsetDateTime

/**
 * DTO for full post details.
 *
 * Returned when fetching a single post by slug or ID. Contains all available information including
 * content and metadata.
 */
data class PostResponse(
    val id: String,
    val title: String,
    val slug: String,
    val content: String,
    val status: PostStatus,
    val publishedAt: OffsetDateTime?,
    val author: String,
    val category: String,
    val tags: List<String>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)

fun Post.toResponse() =
    PostResponse(
        id = this.id.toString(),
        title = this.title.value,
        slug = this.slug.value,
        content = this.content.value,
        status = this.status,
        publishedAt = this.publishedAt,
        author = this.author.username.value,
        category = this.category.name.value,
        tags = this.tags.map { it.name.value },
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
