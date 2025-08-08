package br.dev.demoraes.abrolhos.infrastructure.web.dto.response

import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import java.time.OffsetDateTime

data class PostResponse(
    val title: String,
    val slug: String,
    val content: String,
    val status: PostStatus,
    val authorUsername: String,
    val categoryName: String?,
    val tagNames: Set<String>,
    val updatedAt: OffsetDateTime,
    val createdAt: OffsetDateTime,
    val publishedAt: OffsetDateTime?
)
