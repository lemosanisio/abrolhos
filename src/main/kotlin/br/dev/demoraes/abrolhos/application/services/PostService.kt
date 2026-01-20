package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.domain.entities.Category
import br.dev.demoraes.abrolhos.domain.entities.CategoryName
import br.dev.demoraes.abrolhos.domain.entities.CategorySlug
import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.PostContent
import br.dev.demoraes.abrolhos.domain.entities.PostSlug
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostSummary
import br.dev.demoraes.abrolhos.domain.entities.PostTitle
import br.dev.demoraes.abrolhos.domain.entities.Tag
import br.dev.demoraes.abrolhos.domain.entities.TagName
import br.dev.demoraes.abrolhos.domain.entities.TagSlug
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.repository.CategoryRepository
import br.dev.demoraes.abrolhos.domain.repository.PostRepository
import br.dev.demoraes.abrolhos.domain.repository.TagRepository
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ulid.ULID
import java.time.OffsetDateTime

@Service
@Transactional
class PostService(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository
) {
    private val logger = LoggerFactory.getLogger(PostService::class.java)

    fun createPost(
        title: String,
        content: String,
        status: PostStatus,
        categoryName: String,
        tagNames: List<String>,
        authorUsername: String
    ): Post {
        logger.debug("Creating post: title={}, author={}", title, authorUsername)
        val author = userRepository.findByUsername(Username(authorUsername))
            ?: throw NoSuchElementException("Author not found").also {
                logger.error("Author not found: {}", authorUsername)
            }

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

        return postRepository.save(post).also {
            logger.debug("Post saved with slug: {}", it.slug.value)
        }
    }

    fun findBySlug(slug: String): Post {
        logger.debug("Finding post by slug: {}", slug)
        // Assumes a findBySlug method exists on your PostRepository
        return postRepository.findPublishedBySlug(slug)
            ?: throw NoSuchElementException("Post with slug '$slug' not found").also {
                logger.warn("Post with slug '{}' not found", slug)
            }
    }

    fun searchPostsSummary(
        pageable: Pageable,
        categoryName: String?,
        tagName: String?,
        status: PostStatus
    ): Page<PostSummary> {
        logger.debug(
            "Searching for posts summary with pageable: {}, category: {}, tag: {}, status: {}",
            pageable,
            categoryName,
            tagName,
            status
        )
        return postRepository.searchSummary(pageable, categoryName, tagName, status)
    }

    private fun findOrCreateCategory(name: String): Category {
        val categoryName = CategoryName(name)
        return categoryRepository.findByName(categoryName) ?: run {
            logger.info("Category '{}' not found, creating new one", name)
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
            logger.info("Tag '{}' not found, creating new one", name)
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
