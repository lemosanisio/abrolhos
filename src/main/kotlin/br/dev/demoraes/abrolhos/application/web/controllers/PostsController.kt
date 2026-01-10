package br.dev.demoraes.abrolhos.application.web.controllers

import br.dev.demoraes.abrolhos.application.web.dto.request.CreatePostRequest
import br.dev.demoraes.abrolhos.application.web.dto.response.PostResponse
import br.dev.demoraes.abrolhos.application.web.dto.response.PostSummaryResponse
import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostSummary
import br.dev.demoraes.abrolhos.domain.services.PostService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/posts")
class PostsController(
    private val postService: PostService
) {
    private val logger = LoggerFactory.getLogger(PostsController::class.java)

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun getPostsSummary(
        pageable: Pageable,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) tag: String?,
        @RequestParam(required = false, defaultValue = "PUBLISHED") status: PostStatus,
    ): PagedModel<PostSummaryResponse> {
        logger.info("Received request to get posts with pageable: {}, category: {}, tag: {}, status: {}", pageable, category, tag, status)
        val posts = postService.searchPostsSummary(pageable, category, tag, status).map { it.toResponse() }
        return PagedModel(posts)
    }

    @GetMapping("/{slug}")
    @ResponseStatus(HttpStatus.OK)
    fun getPostBySlug(@PathVariable slug: String): PostResponse {
        logger.info("Received request to get post by slug: {}", slug)
        val post = postService.findBySlug(slug)
        return post.toResponse()
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPost(
        @RequestBody request: CreatePostRequest,
    ): PostResponse {
        logger.info("Received request to create post with title: {}", request.title.value)
        val post = postService.createPost(
            title = request.title.value,
            content = request.content.value,
            status = request.status,
            categoryName = request.categoryName.name.value,
            tagNames = request.tagNames.map { it.name.value },
            authorUsername = request.authorUsername.value
        )
        logger.info("Post created successfully with ID: {}", post.id)
        return post.toResponse()
    }

    private fun Post.toResponse() = PostResponse(
        id = this.id.toString(),
        title = this.title.value,
        slug = this.slug.value,
        content = this.content.value,
        status = this.status,
        publishedAt = this.publishedAt,
        author = this.author.username.value,
        category = this.category.name.value,
        tags = this.tags.map { it.name.value },
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )

    private fun PostSummary.toResponse() = PostSummaryResponse(
        id = this.id,
        author = this.authorUsername,
        title = this.title,
        slug = this.slug,
        category = this.categoryName,
        tags = emptyList(), // Tags not included in summary for now
        shortContent = this.shortContent,
        publishedAt = this.publishedAt
    )
}
