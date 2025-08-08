package br.dev.demoraes.abrolhos.domain.services

import br.dev.demoraes.abrolhos.domain.entities.CategoryName
import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.PostContent
import br.dev.demoraes.abrolhos.domain.entities.PostSlug
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostTitle
import br.dev.demoraes.abrolhos.domain.entities.TagName
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.repository.CategoryRepository
import br.dev.demoraes.abrolhos.domain.repository.PostRepository
import br.dev.demoraes.abrolhos.domain.repository.TagRepository
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import br.dev.demoraes.abrolhos.infrastructure.web.dto.request.CreatePostRequest
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
    private val tagRepository: TagRepository
) {
// TODO(Remove double bangs)
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    fun createDraft(request: CreatePostRequest, authorUsername: String): Post {
        val usernameVO = Username(authorUsername)
        val categoryVO = CategoryName(request.categoryName!!)
        val tagsVO = request.tagNames?.map { TagName(it) }

        val author = userRepository.findByUsername(usernameVO) ?: throw NoSuchElementException("Author '$authorUsername' not found.")

        val category = categoryRepository.findByName(categoryVO)
            ?: throw NoSuchElementException("Category '${request.categoryName}' not found.")

        val tags = tagsVO?.map { tagRepository.findByName(it) ?: throw NoSuchElementException("Tag '${it.value}' not found.") }

        val newPost = Post.create(
            author = author,
            title = PostTitle(request.title),
            slug = PostSlug(request.slug),
            content = PostContent(request.content),
            category = category,
            tags = tags!!.toSet(),
        )

        return postRepository.save(newPost)
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    fun publishPost(postId: String): Post {
        val post = postRepository.findById(ULID.parseULID(postId)) ?: throw NoSuchElementException("Post with ID '$postId' not found.")

        check(post.status == PostStatus.DRAFT) {
            "Only posts in DRAFT status can be published."
        }

        val postToPublish = post.copy(
            status = PostStatus.PUBLISHED,
            publishedAt = OffsetDateTime.now()
        )

        return postRepository.save(postToPublish)
    }
}
