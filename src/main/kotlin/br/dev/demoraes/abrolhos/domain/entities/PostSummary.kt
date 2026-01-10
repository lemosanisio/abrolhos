package br.dev.demoraes.abrolhos.domain.entities

import java.time.OffsetDateTime

interface PostSummary {
    val id: String
    val authorUsername: String
    val title: String
    val slug: String
    val categoryName: String
    val shortContent: String
    val publishedAt: OffsetDateTime
}
