package br.dev.demoraes.abrolhos.domain.entities

import com.fasterxml.jackson.annotation.JsonValue
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

@JvmInline
value class TagName(@get:JsonValue val value: String) {
    companion object {
        const val MIN_LENGTH = 2
        const val MAX_LENGTH = 100
    }

    init {
        require(value.isNotBlank()) { "Tag name cannot be blank" }
        require(value.length in MIN_LENGTH..MAX_LENGTH) {
            "Tag name must be between $MIN_LENGTH and $MAX_LENGTH characters"
        }
    }
}

@JvmInline
value class TagSlug(@get:JsonValue val value: String) {
    companion object {
        const val MIN_LENGTH = 2
        const val MAX_LENGTH = 100
        val SLUG_REGEX = Regex("^[a-z0-9-]+$")
    }

    init {
        require(value.isNotBlank()) { "Tag slug cannot be blank" }
        require(value.length in MIN_LENGTH..MAX_LENGTH) {
            "Tag slug must be between $MIN_LENGTH and $MAX_LENGTH characters"
        }
        require(SLUG_REGEX.matches(value)) {
            "Tag slug can only contain lowercase letters, numbers, and hyphens"
        }
    }
}
