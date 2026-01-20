package br.dev.demoraes.abrolhos.infrastructure.web.dto.response

import java.time.OffsetDateTime

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
