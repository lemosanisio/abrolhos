package br.dev.demoraes.abrolhos.test

import br.dev.demoraes.abrolhos.domain.entities.Category
import br.dev.demoraes.abrolhos.domain.entities.CategoryName
import br.dev.demoraes.abrolhos.domain.entities.CategorySlug
import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.PostContent
import br.dev.demoraes.abrolhos.domain.entities.PostSlug
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostTitle
import br.dev.demoraes.abrolhos.domain.entities.Role
import br.dev.demoraes.abrolhos.domain.entities.Tag
import br.dev.demoraes.abrolhos.domain.entities.TagName
import br.dev.demoraes.abrolhos.domain.entities.TagSlug
import br.dev.demoraes.abrolhos.domain.entities.TotpSecret
import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import ulid.ULID
import java.time.OffsetDateTime

/** Shared Kotest [Arb] generators for domain entities used in property-based tests. */
object TestArbitraries {

    /** Generates a valid [User] with a random (but valid) username and an arbitrary [Role]. */
    fun user(): Arb<User> = arbitrary {
        val suffix = Arb.int(1_000..9_999).bind()
        User(
            id = ULID.nextULID(),
            username = Username("user$suffix"),
            totpSecret = TotpSecret("JBSWY3DPEHPK3PXP"),
            passwordHash = null,
            isActive = true,
            role = Arb.enum<Role>().bind(),
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now(),
        )
    }

    /** Generates a valid [Category]. */
    fun category(): Arb<Category> = arbitrary {
        val slug = "cat-${Arb.int(1_000..9_999).bind()}"
        Category(
            id = ULID.nextULID(),
            name = CategoryName("Category $slug"),
            slug = CategorySlug(slug),
            posts = emptySet(),
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now(),
        )
    }

    /** Generates a valid [Tag]. */
    fun tag(): Arb<Tag> = arbitrary {
        val slug = "tag-${Arb.int(1_000..9_999).bind()}"
        Tag(
            id = ULID.nextULID(),
            name = TagName("Tag $slug"),
            slug = TagSlug(slug),
            posts = emptySet(),
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now(),
        )
    }

    /** Generates a valid [PostTitle] (3–60 characters, no leading/trailing spaces). */
    fun postTitle(): Arb<PostTitle> = arbitrary {
        val n = Arb.int(3..60).bind()
        // Use alphanumeric chars only to avoid value-class validation failures
        val chars = ('a'..'z') + ('0'..'9') + listOf(' ')
        val raw =
            (1..n).map { chars.random() }.joinToString("").trim().let {
                if (it.length < 3) "abc" else it
            }
        PostTitle(raw)
    }

    /** Generates a [PostContent] with between 1 and 500 characters. */
    fun postContent(): Arb<PostContent> = arbitrary {
        val n = Arb.int(1..500).bind()
        PostContent(Arb.string(n, n).bind().ifBlank { "x".repeat(n) })
    }

    /**
     * Generates a full [Post] with a random author, category, tags, title, and status. The slug is
     * derived from a simple fixed pattern to keep it valid.
     */
    fun post(): Arb<Post> = arbitrary {
        val author = user().bind()
        val cat = category().bind()
        val status = Arb.enum<PostStatus>().bind()
        val slug = "post-${Arb.int(10_000..99_999).bind()}"
        // Pick 0–2 tags without using Arb.subset (unavailable in Kotest 5.8)
        val allTags = listOf(tag().bind(), tag().bind())
        val tagCount = Arb.int(0..2).bind()
        val tagSet = allTags.take(tagCount).toSet()
        Post(
            id = ULID.nextULID(),
            author = author,
            title = PostTitle("Post $slug"),
            slug = PostSlug(slug),
            content = PostContent("Content for $slug"),
            status = status,
            publishedAt =
            if (status == PostStatus.PUBLISHED || status == PostStatus.SCHEDULED) {
                OffsetDateTime.now().minusHours(1)
            } else {
                null
            },
            category = cat,
            tags = tagSet,
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now(),
        )
    }
}
