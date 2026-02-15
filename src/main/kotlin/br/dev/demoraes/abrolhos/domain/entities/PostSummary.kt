package br.dev.demoraes.abrolhos.domain.entities

import java.time.OffsetDateTime

/**
 * Projection interface for listing post summaries.
 *
 * This interface is used by Spring Data JPA to project query results into a lightweight view,
 * suitable for listing pages where full content is not required.
 */
interface PostSummary {
    val id: String
    val authorUsername: String
    val title: String
    val slug: String
    val categoryName: String
    val shortContent: String
    val publishedAt: OffsetDateTime
}
