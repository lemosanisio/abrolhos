package br.dev.demoraes.abrolhos.infrastructure.web.dto.response

import br.dev.demoraes.abrolhos.domain.entities.PostSummary
import java.time.OffsetDateTime

/**
 * DTO for post summary lists.
 *
 * Returned when searching or listing posts. specific lightweight view of a post, excluding the full
 * content.
 */
data class PostSummaryResponse(
    val id: String,
    val author: String,
    val title: String,
    val slug: String,
    val category: String,
    val tags: List<String>,
    val shortContent: String,
    val publishedAt: OffsetDateTime
)

fun PostSummary.toResponse() =
    PostSummaryResponse(
        id = this.id,
        author = this.authorUsername,
        title = this.title,
        slug = this.slug,
        category = this.categoryName,
        tags = emptyList(), // Tags not included in summary for now
        shortContent = this.shortContent,
        publishedAt = this.publishedAt
    )
