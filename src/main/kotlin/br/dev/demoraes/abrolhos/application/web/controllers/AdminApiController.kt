package br.dev.demoraes.abrolhos.application.web.controllers

import br.dev.demoraes.abrolhos.Application.api.AdminApi
import br.dev.demoraes.abrolhos.domain.entities.toGeneratedResponse
import br.dev.demoraes.abrolhos.domain.services.PostService
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import br.dev.demoraes.abrolhos.Application.dto.PostResponse as GeneratedPostResponse

@Controller
@RequestMapping("/api/v1")
class AdminApiController(
    private val postService: PostService,
) : AdminApi {
    override fun adminListPosts(): ResponseEntity<List<GeneratedPostResponse>> {
        val posts = postService.listAllPosts().map { it.toGeneratedResponse() }
        return ResponseEntity.ok(posts)
    }
}
