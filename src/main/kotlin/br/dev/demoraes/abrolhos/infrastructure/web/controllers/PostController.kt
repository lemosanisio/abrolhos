package br.dev.demoraes.abrolhos.infrastructure.web.controllers

import br.dev.demoraes.abrolhos.domain.entities.toResponse
import br.dev.demoraes.abrolhos.domain.services.PostService
import br.dev.demoraes.abrolhos.infrastructure.web.dto.request.CreatePostRequest
import br.dev.demoraes.abrolhos.infrastructure.web.dto.response.PostResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/api/v1/posts")
class PostController(
    private val postService: PostService
) {

    @PostMapping
    fun createPost(
        @RequestBody request: CreatePostRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<PostResponse> {
        val createdPostDomain = postService.createDraft(request, userDetails.username)
        val responseDto = createdPostDomain.toResponse()

        // Return a 201 Created status with the location of the new resource
        return ResponseEntity
            .created(URI.create("/api/public/posts/${responseDto.slug}"))
            .body(responseDto)
    }

    @PostMapping("/{id}/publish")
    fun publishPost(@PathVariable id: String): ResponseEntity<PostResponse> {
        val publishedPostDomain = postService.publishPost(id)
        return ResponseEntity.ok(publishedPostDomain.toResponse())
    }
}
