package br.dev.demoraes.abrolhos.application.web.controllers

import br.dev.demoraes.abrolhos.application.web.dto.request.CreatePostRequest
import br.dev.demoraes.abrolhos.application.web.dto.response.PostResponse
import br.dev.demoraes.abrolhos.domain.entities.toResponse
import br.dev.demoraes.abrolhos.domain.services.PostService
import br.dev.demoraes.abrolhos.domain.services.commands.CreatePostCommand
import br.dev.demoraes.abrolhos.infrastructure.security.UserPrincipal
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/legacy/v1/posts")
class PostController(
    private val postService: PostService,
) {
    @PostMapping
    fun createPost(
        @Valid @RequestBody request: CreatePostRequest,
        @AuthenticationPrincipal principal: UserPrincipal,
    ): ResponseEntity<PostResponse> {
        val command =
            CreatePostCommand(
                title = request.title,
                content = request.content,
                categoryName = request.categoryName,
                tagNames = request.tagNames,
            )
        val createdPostDomain = postService.createDraft(command, principal.user.username.value)
        val responseDto = createdPostDomain.toResponse()

        // Return a 201 Created status with the location of the new resource
        return ResponseEntity
            .created(URI.create("/api/public/posts/${responseDto.slug}"))
            .body(responseDto)
    }

    @PostMapping("/{id}/publish")
    fun publishPost(
        @PathVariable id: String,
    ): ResponseEntity<PostResponse> {
        val publishedPostDomain = postService.publishPost(id)
        return ResponseEntity.ok(publishedPostDomain.toResponse())
    }

    @PostMapping("/{id}/archive")
    fun archivePost(
        @PathVariable id: String,
    ): ResponseEntity<PostResponse> {
        val archived = postService.archivePost(id)
        return ResponseEntity.ok(archived.toResponse())
    }
}
