package br.dev.demoraes.abrolhos.domain.services

import br.dev.demoraes.abrolhos.domain.entities.*
import br.dev.demoraes.abrolhos.domain.repository.CategoryRepository
import br.dev.demoraes.abrolhos.domain.repository.PostRepository
import br.dev.demoraes.abrolhos.domain.repository.TagRepository
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import ulid.ULID
import java.time.OffsetDateTime
import java.util.*

class PostService(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository
) {
    fun createPost(
        title: String,
        content: String,
        status: PostStatus,
        categoryName: String,
        tagNames: List<String>,
        authorUsername: String
    ): Post {
        val author = userRepository.findByUsername(Username(authorUsername))
            ?: throw NoSuchElementException("Author not found")

        val slug = generateSlug(title)

        val category = findOrCreateCategory(categoryName)
        val tags = tagNames.map { findOrCreateTag(it) }.toSet()

        val post = Post(
            id = ULID.nextULID(),
            author = author,
            title = PostTitle(title),
            slug = PostSlug(slug),
            content = PostContent(content),
            status = status,
            category = category,
            tags = tags,
            publishedAt = if (status == PostStatus.PUBLISHED) OffsetDateTime.now() else null,
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )

        return postRepository.save(post)
    }

    private fun findOrCreateCategory(name: String): Category {
        val categoryName = CategoryName(name)
        return categoryRepository.findByName(categoryName) ?: run {
            val slug = generateSlug(name)
            categoryRepository.save(
                Category(
                    id = ULID.nextULID(),
                    name = categoryName,
                    slug = CategorySlug(slug),
                    posts = emptySet(),
                    createdAt = OffsetDateTime.now(),
                    updatedAt = OffsetDateTime.now()
                )
            )
        }
    }

    private fun findOrCreateTag(name: String): Tag {
        val tagName = TagName(name)
        return tagRepository.findByName(tagName) ?: run {
            val slug = generateSlug(name)
            tagRepository.save(
                Tag(
                    id = ULID.nextULID(),
                    name = tagName,
                    slug = TagSlug(slug),
                    posts = emptySet(),
                    createdAt = OffsetDateTime.now(),
                    updatedAt = OffsetDateTime.now()
                )
            )
        }
    }

    private fun generateSlug(text: String): String {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace(Regex("\\s+"), "-")
            .trim('-')
    }
}
