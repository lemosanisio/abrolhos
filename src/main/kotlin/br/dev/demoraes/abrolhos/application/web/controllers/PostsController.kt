package br.dev.demoraes.abrolhos.application.web.controllers

import br.dev.demoraes.abrolhos.application.web.dto.request.PostCreateRequest
import br.dev.demoraes.abrolhos.application.web.dto.response.PostResponse
import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.services.PostService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/posts")
class PostsController(
    private val postService: PostService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPost(
        @RequestBody request: PostCreateRequest,
        principal: Principal
    ): PostResponse {
        val post = postService.createPost(
            title = request.title,
            content = request.content,
            status = request.status,
            categoryName = request.category.name,
            tagNames = request.tags.map { it.name },
            authorUsername = principal.name
        )
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
}
