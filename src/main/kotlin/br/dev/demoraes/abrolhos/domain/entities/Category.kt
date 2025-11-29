package br.dev.demoraes.abrolhos.domain.entities

import ulid.ULID
import java.time.OffsetDateTime

data class Category(
    val id: ULID,
    val name: CategoryName,
    val slug: CategorySlug,
    val posts: Set<Post>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

// TODO(Should finish defining those value objects here; at least length)
@JvmInline
value class CategoryName(val value: String) {
    init {
        require(value.isNotBlank()) { "Category name cannot be blank" }
    }
}

@JvmInline
value class CategorySlug(val value: String) {
    init {
        require(value.isNotBlank()) { "Category slug cannot be blank" }
    }
}
