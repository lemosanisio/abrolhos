package br.dev.demoraes.abrolhos.domain.authentication.entities

import java.time.OffsetDateTime

data class Post(
    val author: User,
    val title: String,
    val slug: String,
    val content: String,
    val status: PostStatus,
    val publishedAt: OffsetDateTime,
    val category: Category,
    val tags: MutableSet<Tag>
)

enum class PostStatus {
    DRAFT, PUBLISHED, SCHEDULED
}
