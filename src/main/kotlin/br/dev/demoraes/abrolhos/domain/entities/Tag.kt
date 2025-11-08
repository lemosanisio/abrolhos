package br.dev.demoraes.abrolhos.domain.entities

import ulid.ULID
import java.time.OffsetDateTime

data class Tag(
    val id: ULID,
    val name: TagName,
    val slug: TagSlug,
    val posts: Set<Post>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

// TODO(Should finish defining those value objects here; at least length)
@JvmInline
value class TagName(val value: String) {
    init {
        require(value.isNotBlank()) { "Tag name cannot be blank" }
    }
}

@JvmInline
value class TagSlug(val value: String) {
    init {
        require(value.isNotBlank()) { "Tag slug cannot be blank" }
    }
}
