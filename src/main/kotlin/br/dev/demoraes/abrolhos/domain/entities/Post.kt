package br.dev.demoraes.abrolhos.domain.entities

import com.fasterxml.jackson.annotation.JsonValue
import ulid.ULID
import java.time.OffsetDateTime

data class Post(
    val id: ULID,
    val author: User,
    val title: PostTitle,
    val slug: PostSlug,
    val content: PostContent,
    val status: PostStatus,
    val publishedAt: OffsetDateTime?,
    val category: Category,
    val tags: Set<Tag>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    companion object {
        fun create(
            author: User,
            title: PostTitle,
            slug: PostSlug,
            content: PostContent,
            category: Category,
            tags: Set<Tag>,
        ): Post {
            return Post(
                id = ULID.nextULID(),
                author = author,
                title = title,
                slug = slug,
                content = content,
                status = PostStatus.DRAFT,
                category = category,
                tags = tags,
                createdAt = OffsetDateTime.now(), // Set initial timestamps
                updatedAt = OffsetDateTime.now(),
                publishedAt = null,
            )
        }
    }
}

enum class PostStatus {
    DRAFT,
    PUBLISHED,
    SCHEDULED,
    ARCHIVED,
}

@JvmInline
value class PostTitle(@get:JsonValue val value: String) {
    companion object {
        const val MIN_LENGTH = 3
        const val MAX_LENGTH = 255
    }

    init {
        require(value.isNotBlank()) { "Post title cannot be blank" }
        require(value.length in MIN_LENGTH..MAX_LENGTH) {
            "Post title must be between $MIN_LENGTH and $MAX_LENGTH characters"
        }
    }
}

@JvmInline
value class PostSlug(@get:JsonValue val value: String) {
    companion object {
        const val MIN_LENGTH = 3
        const val MAX_LENGTH = 255
        val SLUG_REGEX = Regex("^[a-z0-9-]+$")
    }

    init {
        require(value.isNotBlank()) { "Post slug cannot be blank" }
        require(value.length in MIN_LENGTH..MAX_LENGTH) {
            "Post slug must be between $MIN_LENGTH and $MAX_LENGTH characters"
        }
        require(SLUG_REGEX.matches(value)) {
            "Post slug can only contain lowercase letters, numbers, and hyphens"
        }
    }
}

@JvmInline
value class PostContent(@get:JsonValue val value: String) {
    companion object {
        const val MIN_LENGTH = 1
    }

    init {
        require(value.isNotBlank()) { "Post content cannot be blank" }
        require(value.length >= MIN_LENGTH) {
            "Post content must be at least $MIN_LENGTH characters"
        }
    }
}
