package br.dev.demoraes.abrolhos.infrastructure.web.controllers

import br.dev.demoraes.abrolhos.application.services.PostService
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.infrastructure.web.dto.request.CreatePostRequest
import br.dev.demoraes.abrolhos.infrastructure.web.dto.response.PostResponse
import br.dev.demoraes.abrolhos.infrastructure.web.dto.response.PostSummaryResponse
import br.dev.demoraes.abrolhos.infrastructure.web.dto.response.toResponse
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * REST Controller for blog post management.
 *
 * Exposes endpoints for:
 * - Listing post summaries with pagination and filtering (public)
 * - Retrieving full post details by slug (public)
 * - Creating new posts (secured, requires authentication)
 */

@RestController
@RequestMapping("/api/posts")
class PostsController(
    private val postService: PostService
) {

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun getPostsSummary(
        pageable: Pageable,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) tag: String?,
        @RequestParam(required = false, defaultValue = "PUBLISHED") status: PostStatus,
    ): PagedModel<PostSummaryResponse> {
        logger.info("Received request to get posts with pageable: $pageable, category: $category, tag: $tag, status: $status")
        val posts = postService.searchPostSummaries(pageable, category, tag, status)
        return PagedModel(posts.map { it.toResponse() })
    }

    @GetMapping("/{slug}")
    @ResponseStatus(HttpStatus.OK)
    fun getPostBySlug(@PathVariable slug: String): PostResponse {
        logger.info("Received request to get post by slug: $slug")
        val post = postService.findBySlug(slug)
        return post.toResponse()
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPost(
        @RequestBody request: CreatePostRequest,
        authentication: Authentication,
    ): PostResponse {
        logger.info("Received request to create post with title: ${request.title.value}")
        val post = postService.createPost(
            title = request.title.value,
            content = request.content.value,
            status = request.status,
            categoryName = request.categoryName.value,
            tagNames = request.tagNames.map { it.value },
            authorUsername = authentication.name
        )
        logger.info("Post created successfully with ID: ${post.id}")
        return post.toResponse()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PostsController::class.java)
    }
}
