package br.dev.demoraes.abrolhos.application.web.controllers

import br.dev.demoraes.abrolhos.Application.api.PostsApi
import br.dev.demoraes.abrolhos.domain.entities.toGeneratedResponse
import br.dev.demoraes.abrolhos.domain.services.PostService
import br.dev.demoraes.abrolhos.domain.services.commands.CreatePostCommand
import br.dev.demoraes.abrolhos.domain.services.commands.UpdatePostCommand
import br.dev.demoraes.abrolhos.infrastructure.security.UserPrincipal
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import java.net.URI
import br.dev.demoraes.abrolhos.Application.dto.CreatePostRequest as GeneratedCreatePostRequest
import br.dev.demoraes.abrolhos.Application.dto.PostResponse as GeneratedPostResponse

@Controller
@RequestMapping("/api/v1")
class PostsApiController(
    private val postService: PostService,
) : PostsApi {
    // HTML view for HTMX/Thymeleaf
    @GetMapping(value = ["/posts/{slug}"], produces = [MediaType.TEXT_HTML_VALUE])
    fun getPostAsHtmx(
        @PathVariable slug: String,
        model: Model,
    ): String {
        val post =
            try {
                postService.getPublishedPostBySlug(slug)
            } catch (_: NoSuchElementException) {
                return "error/404"
            }
        model.addAttribute("post", post.toGeneratedResponse())
        return "fragments/post-detail"
    }

    override fun createPost(createPostRequest: GeneratedCreatePostRequest): ResponseEntity<GeneratedPostResponse> {
        // Obtain current authenticated principal
        val auth = SecurityContextHolder.getContext().authentication
        val username =
            when (val principal = auth?.principal) {
                is UserPrincipal -> principal.user.username.value
                is UserDetails -> principal.username
                is String -> principal
                else -> null
            }

        if (username.isNullOrBlank()) {
            return ResponseEntity.status(401).build()
        }

        val command =
            CreatePostCommand(
                title = createPostRequest.title,
                content = createPostRequest.content,
                categoryName = createPostRequest.categoryName,
                tagNames = createPostRequest.tagNames?.toSet(),
            )

        val created = postService.createDraft(command, username)
        val body = created.toGeneratedResponse()

        val location = "/api/v1/posts/${'$'}{body.slug}"
        return ResponseEntity.created(URI.create(location)).body(body)
    }

    override fun listPublishedPosts(): ResponseEntity<List<GeneratedPostResponse>> {
        val posts = postService.listPublishedPosts().map { it.toGeneratedResponse() }
        return ResponseEntity.ok(posts)
    }

    override fun getPublishedPostBySlug(slug: String): ResponseEntity<GeneratedPostResponse> {
        val post =
            try {
                postService.getPublishedPostBySlug(slug)
            } catch (e: NoSuchElementException) {
                return ResponseEntity.notFound().build()
            }
        return ResponseEntity.ok(post.toGeneratedResponse())
    }

    override fun publishPost(id: String): ResponseEntity<GeneratedPostResponse> {
        val published = postService.publishPost(id)
        return ResponseEntity.ok(published.toGeneratedResponse())
    }

    override fun archivePost(id: String): ResponseEntity<GeneratedPostResponse> {
        val archived = postService.archivePost(id)
        return ResponseEntity.ok(archived.toGeneratedResponse())
    }

    override fun updatePost(
        id: String,
        createPostRequest: GeneratedCreatePostRequest,
    ): ResponseEntity<GeneratedPostResponse> {
        val command =
            UpdatePostCommand(
                title = createPostRequest.title,
                content = createPostRequest.content,
                categoryName = createPostRequest.categoryName,
                tagNames = createPostRequest.tagNames?.toSet(),
            )
        val updated = postService.updatePost(id, command)
        return ResponseEntity.ok(updated.toGeneratedResponse())
    }

    override fun deletePost(id: String): ResponseEntity<Unit> {
        postService.deletePost(id)
        return ResponseEntity.noContent().build()
    }
}
