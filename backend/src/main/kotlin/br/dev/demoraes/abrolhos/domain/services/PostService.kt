package br.dev.demoraes.abrolhos.domain.services

import br.dev.demoraes.abrolhos.domain.entities.CategoryName
import br.dev.demoraes.abrolhos.domain.entities.CategorySlug
import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.PostContent
import br.dev.demoraes.abrolhos.domain.entities.PostSlug
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostTitle
import br.dev.demoraes.abrolhos.domain.entities.TagName
import br.dev.demoraes.abrolhos.domain.entities.TagSlug
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.repository.CategoryRepository
import br.dev.demoraes.abrolhos.domain.repository.PostRepository
import br.dev.demoraes.abrolhos.domain.repository.TagRepository
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import br.dev.demoraes.abrolhos.domain.services.commands.CreatePostCommand
import br.dev.demoraes.abrolhos.domain.services.commands.UpdatePostCommand
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ulid.ULID
import java.time.OffsetDateTime

@Service
class PostService(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val categoryRepository: CategoryRepository,
    private val tagRepository: TagRepository,
) {
    // TODO(Remove double bangs)
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    fun createDraft(
        command: CreatePostCommand,
        authorUsername: String,
    ): Post {
        val usernameVO = Username(authorUsername)
        val categoryVO = CategoryName(command.categoryName!!)
        val tagsVO = command.tagNames?.map { TagName(it) }

        val author =
            userRepository.findByUsername(usernameVO)
                ?: throw NoSuchElementException("Author '$authorUsername' not found.")

        val category =
            categoryRepository.findByName(categoryVO)
                ?: throw NoSuchElementException(
                    "Category '${command.categoryName}' not found."
                )

        val tags =
            tagsVO?.map {
                tagRepository.findByName(it)
                    ?: throw NoSuchElementException("Tag '${it.value}' not found.")
            }

        val slugValue = generateUniqueSlug(command.title)

        val newPost =
            Post.create(
                author = author,
                title = PostTitle(command.title),
                slug = PostSlug(slugValue),
                content = PostContent(command.content),
                category = category,
                tags = tags!!.toSet(),
            )

        return postRepository.save(newPost)
    }

    private fun generateUniqueSlug(title: String): String {
        fun slugify(input: String): String {
            val lower = input.lowercase()
            val replaced = lower.replace("[^a-z0-9]+".toRegex(), "-")
            val collapsed = replaced.replace("-+".toRegex(), "-")
            return collapsed.trim('-')
        }
        var base = slugify(title)
        if (base.isBlank()) base = "post"
        val existing = postRepository.findAll().filterNotNull().map { it.slug.value }.toSet()
        if (base !in existing) return base
        var counter = 2
        while (true) {
            val candidate = "$base-$counter"
            if (candidate !in existing) return candidate
            counter++
        }
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    fun publishPost(postId: String): Post {
        val post =
            postRepository.findById(ULID.parseULID(postId))
                ?: throw NoSuchElementException("Post with ID '$postId' not found.")

        check(post.status in setOf(PostStatus.DRAFT, PostStatus.SCHEDULED)) {
            "Only posts in DRAFT or SCHEDULED status can be published."
        }

        val postToPublish =
            post.copy(
                status = PostStatus.PUBLISHED,
                publishedAt = OffsetDateTime.now(),
            )

        return postRepository.save(postToPublish)
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    fun archivePost(postId: String): Post {
        val post =
            postRepository.findById(ULID.parseULID(postId))
                ?: throw NoSuchElementException("Post with ID '$postId' not found.")

        check(post.status != PostStatus.ARCHIVED) { "Post is already archived." }

        val archived =
            post.copy(
                status = PostStatus.ARCHIVED,
            )

        return postRepository.save(archived)
    }

    @Transactional(readOnly = true)
    fun listPublishedPosts(): Set<Post> {
        return postRepository.findPublished()
    }

    @Transactional(readOnly = true)
    fun getPublishedPostBySlug(slug: String): Post {
        return postRepository.findPublishedBySlug(slug)
            ?: throw NoSuchElementException("Published post with slug '$slug' not found.")
    }

    @Transactional(readOnly = true)
    fun getPublishedPostsByCategory(categorySlug: String): Set<Post> {
        categoryRepository.findBySlug(CategorySlug(categorySlug))
            ?: throw NoSuchElementException("Category with slug '$categorySlug' not found.")
        return postRepository.findPublishedByCategory(categorySlug)
    }

    @Transactional(readOnly = true)
    fun getPublishedPostsByTag(tagSlug: String): Set<Post> {
        tagRepository.findBySlug(TagSlug(tagSlug))
            ?: throw NoSuchElementException("Tag with slug '$tagSlug' not found.")
        return postRepository.findPublishedByTag(tagSlug)
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    fun updatePost(
        postId: String,
        command: UpdatePostCommand,
    ): Post {
        val existing =
            postRepository.findById(ULID.parseULID(postId))
                ?: throw NoSuchElementException("Post with ID '$postId' not found.")

        val category =
            command.categoryName?.let { name ->
                val vo = CategoryName(name)
                categoryRepository.findByName(vo)
                    ?: throw NoSuchElementException(
                        "Category '${command.categoryName}' not found."
                    )
            }
                ?: existing.category

        val tags =
            command.tagNames
                ?.map { TagName(it) }
                ?.map { tagName ->
                    tagRepository.findByName(tagName)
                        ?: throw NoSuchElementException(
                            "Tag '${tagName.value}' not found."
                        )
                }
                ?.toSet()
                ?: existing.tags

        val titleVO = PostTitle(command.title)
        val contentVO = PostContent(command.content)
        val newSlug =
            if (titleVO.value != existing.title.value) {
                PostSlug(
                    generateUniqueSlug(titleVO.value),
                )
            } else {
                existing.slug
            }

        val updated =
            existing.copy(
                title = titleVO,
                slug = newSlug,
                content = contentVO,
                category = category,
                tags = tags,
            )

        return postRepository.save(updated)
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    fun deletePost(postId: String) {
        archivePost(postId)
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    fun listAllPosts(): Set<Post> = postRepository.findAll()
}
