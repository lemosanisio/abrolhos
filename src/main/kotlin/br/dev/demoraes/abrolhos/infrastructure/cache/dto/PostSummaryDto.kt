package br.dev.demoraes.abrolhos.infrastructure.cache.dto

import br.dev.demoraes.abrolhos.domain.entities.PostSummary
import java.time.OffsetDateTime

/**
 * Concrete, serializable implementation of [PostSummary] for Redis caching.
 *
 * Spring Data JPA returns a proxy object when projecting into the [PostSummary] interface. That
 * proxy is not serializable, so this data class acts as a concrete, Jackson-serializable
 * representation that can safely be stored in and retrieved from Redis.
 *
 * Mapping from the JPA proxy to this DTO happens in [PostRepositoryImpl.searchSummary].
 */
data class PostSummaryDto(
    override val id: String,
    override val authorUsername: String,
    override val title: String,
    override val slug: String,
    override val categoryName: String,
    override val shortContent: String,
    override val publishedAt: OffsetDateTime,
) : PostSummary
