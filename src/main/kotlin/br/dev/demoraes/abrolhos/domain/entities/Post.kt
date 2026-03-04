package br.dev.demoraes.abrolhos.domain.entities

import com.fasterxml.jackson.annotation.JsonValue
import ulid.ULID
import java.time.OffsetDateTime

/**
 * Represents a blog post.
 *
 * This is the core content entity. It aggregates the author, content, status, category, and tags.
 *
 * @property id Unique identifier (ULID)
 * @property author The user who created the post
 * @property title The title of the post
 * @property slug URL-friendly identifier
 * @property content The main body of the post
 * @property status Current state (DRAFT, PUBLISHED, etc.)
 * @property category The category this post belongs to
 * @property tags Set of tags associated with the post
 */
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

    /** Returns true if the given user ID matches the post author's ID. */
    fun isOwnedBy(userId: ULID): Boolean = author.id == userId

    /**
     * Returns a new [Post] with the specified fields replaced, and [updatedAt] set to now. Any
     * parameter left null keeps its current value.
     */
    fun withUpdatedFields(
        title: PostTitle? = null,
        slug: PostSlug? = null,
        content: PostContent? = null,
        status: PostStatus? = null,
        category: Category? = null,
        tags: Set<Tag>? = null,
        publishedAt: OffsetDateTime? = null,
    ): Post =
        copy(
            title = title ?: this.title,
            slug = slug ?: this.slug,
            content = content ?: this.content,
            status = status ?: this.status,
            category = category ?: this.category,
            tags = tags ?: this.tags,
            publishedAt = publishedAt ?: this.publishedAt,
            updatedAt = OffsetDateTime.now(),
        )
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
