package br.dev.demoraes.abrolhos.application.web.dto.response

import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import java.time.OffsetDateTime

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
