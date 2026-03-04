package br.dev.demoraes.abrolhos.infrastructure.web.controllers

import br.dev.demoraes.abrolhos.application.services.PostService
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.Role
import br.dev.demoraes.abrolhos.infrastructure.web.dto.request.CreatePostRequest
import br.dev.demoraes.abrolhos.infrastructure.web.dto.request.UpdatePostRequest
import br.dev.demoraes.abrolhos.infrastructure.web.dto.response.PostResponse
import br.dev.demoraes.abrolhos.infrastructure.web.dto.response.PostSummaryResponse
import br.dev.demoraes.abrolhos.infrastructure.web.dto.response.toResponse
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
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
 * - Retrieving full post details by slug (public for published; owner/admin for unpublished)
 * - Creating new posts (requires authentication)
 * - Updating posts (requires authentication; owner or ADMIN)
 * - Deleting posts (requires authentication; owner or ADMIN)
 */
@RestController
@RequestMapping("/api/posts")
class PostsController(private val postService: PostService) {

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun getPostsSummary(
        pageable: Pageable,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) tag: String?,
        @RequestParam(required = false, defaultValue = "PUBLISHED") status: PostStatus,
    ): PagedModel<PostSummaryResponse> {
        logger.info(
            "Received request to get posts with pageable: $pageable, category: $category, tag: $tag, status: $status"
        )
        val posts = postService.searchPostSummaries(pageable, category, tag, status)
        return PagedModel(posts.map { it.toResponse() })
    }

    @GetMapping("/cursor")
    @ResponseStatus(HttpStatus.OK)
    fun getPostsSummaryByCursor(
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): br.dev.demoraes.abrolhos.infrastructure.web.dto.response.CursorPageResponse<
        PostSummaryResponse
        > {
        logger.info("Received cursor pagination request: cursor=$cursor, size=$size")
        val page = postService.searchPostSummariesByCursor(cursor, size, PostStatus.PUBLISHED)
        return br.dev.demoraes.abrolhos.infrastructure.web.dto.response.CursorPageResponse(
            items = page.items.map { it.toResponse() },
            nextCursor = page.nextCursor,
            hasNext = page.hasNext,
        )
    }

    @GetMapping("/{slug}")
    @ResponseStatus(HttpStatus.OK)
    fun getPostBySlug(
        @PathVariable slug: String,
        authentication: Authentication?,
    ): PostResponse {
        logger.info("Received request to get post by slug: $slug")
        val post =
            postService.findBySlugForUser(
                slug = slug,
                currentUsername = authentication?.name,
                currentUserRole = authentication.extractRole(),
            )
        return post.toResponse()
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPost(
        @RequestBody request: CreatePostRequest,
        authentication: Authentication,
    ): PostResponse {
        logger.info("Received request to create post with title: ${request.title.value}")
        val post =
            postService.createPost(
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

    @PutMapping("/{slug}")
    @ResponseStatus(HttpStatus.OK)
    fun updatePost(
        @PathVariable slug: String,
        @RequestBody request: UpdatePostRequest,
        authentication: Authentication,
    ): PostResponse {
        logger.info("Received request to update post: $slug")
        val post =
            postService.updatePost(
                slug = slug,
                title = request.title?.value,
                content = request.content?.value,
                status = request.status,
                categoryName = request.categoryName?.value,
                tagNames = request.tagNames?.map { it.value },
                currentUsername = authentication.name,
                currentUserRole = authentication.extractRole() ?: Role.USER,
            )
        return post.toResponse()
    }

    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePost(
        @PathVariable slug: String,
        authentication: Authentication,
    ) {
        logger.info("Received request to delete post: $slug")
        postService.deletePost(
            slug = slug,
            currentUsername = authentication.name,
            currentUserRole = authentication.extractRole() ?: Role.USER,
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PostsController::class.java)
    }
}

/** Extracts the user's [Role] from the granted authorities, or null if not present. */
private fun Authentication?.extractRole(): Role? =
    this?.authorities
        ?.firstOrNull { it.authority.startsWith("ROLE_") }
        ?.authority
        ?.removePrefix("ROLE_")
        ?.let { runCatching { Role.valueOf(it) }.getOrNull() }
