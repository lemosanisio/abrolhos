package br.dev.demoraes.abrolhos.application.web.controllers

import br.dev.demoraes.abrolhos.application.api.TagsApi
import br.dev.demoraes.abrolhos.application.dto.PostResponse
import br.dev.demoraes.abrolhos.application.dto.TagRequest
import br.dev.demoraes.abrolhos.application.dto.TagResponse
import br.dev.demoraes.abrolhos.application.web.dto.mappers.DtoMappers.toResponse
import br.dev.demoraes.abrolhos.domain.entities.toGeneratedResponse
import br.dev.demoraes.abrolhos.domain.services.PostService
import br.dev.demoraes.abrolhos.domain.services.TagService
import br.dev.demoraes.abrolhos.domain.services.commands.CreateTagCommand
import br.dev.demoraes.abrolhos.domain.services.commands.UpdateTagCommand
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.net.URI

@Controller
@RequestMapping("/api/v1")
class TagsApiController(
    private val tagService: TagService,
    private val postService: PostService,
    private val templateEngine: TemplateEngine,
) : TagsApi {
    override fun listTags(): ResponseEntity<List<TagResponse>> =
        ResponseEntity.ok(tagService.listTags().map { it.toResponse() })

    override fun getTagBySlug(slug: String): ResponseEntity<TagResponse> =
        tagService.getBySlug(slug)?.let { ResponseEntity.ok(it.toResponse()) }
            ?: ResponseEntity.notFound().build()

    override fun getPostsByTag(slug: String): ResponseEntity<List<PostResponse>> {
        val posts = postService.getPublishedPostsByTag(slug)
        return ResponseEntity.ok(posts.map { it.toGeneratedResponse() })
    }

    // HTMX Methods

    @GetMapping(value = ["/tags"], produces = [MediaType.TEXT_HTML_VALUE])
    fun listTagsHtmx(): ResponseEntity<String> {
        val tags = tagService.listTags().map { it.toResponse() }
        val context = Context()
        context.setVariable("tags", tags)
        val html = templateEngine.process("tags/list", context)
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html)
    }

    @GetMapping(value = ["/tags/{slug}"], produces = [MediaType.TEXT_HTML_VALUE])
    fun getTagHtmx(
        @PathVariable slug: String,
        @org.springframework.web.bind.annotation.RequestHeader(
            value = "HX-Request",
            required = false
        )
        hxRequest: String?
    ): ResponseEntity<String> {
        val tag = tagService.getBySlug(slug)?.toResponse()
        return if (tag != null) {
            val context = Context()
            context.setVariable("tag", tag)
            val viewName = if (hxRequest == "true") "fragments/tag-detail" else "tags/detail"
            val html = templateEngine.process(viewName, context)
            ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html)
        } else {
            val html = templateEngine.process("error/404", Context())
            ResponseEntity.status(404).contentType(MediaType.TEXT_HTML).body(html)
        }
    }

    @GetMapping(value = ["/tags/{slug}/posts"], produces = [MediaType.TEXT_HTML_VALUE])
    fun getPostsByTagHtmx(
        @PathVariable slug: String,
        @org.springframework.web.bind.annotation.RequestHeader(
            value = "HX-Request",
            required = false
        )
        hxRequest: String?
    ): ResponseEntity<String> {
        val posts = postService.getPublishedPostsByTag(slug).map { it.toGeneratedResponse() }
        val context = Context()
        context.setVariable("posts", posts)
        context.setVariable("slug", slug)
        val viewName = if (hxRequest == "true") "fragments/tag-posts" else "tags/posts"
        val html = templateEngine.process(viewName, context)
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html)
    }

    override fun createTag(tagRequest: TagRequest): ResponseEntity<TagResponse> {
        val command = CreateTagCommand(name = tagRequest.name)
        val created = tagService.create(command)
        val response = created.toResponse()
        return ResponseEntity.created(URI.create("/api/v1/tags/${response.slug}")).body(response)
    }

    override fun updateTag(
        id: String,
        tagRequest: TagRequest,
    ): ResponseEntity<TagResponse> {
        val command = UpdateTagCommand(id = id, name = tagRequest.name)
        val updated = tagService.update(command)
        return ResponseEntity.ok(updated.toResponse())
    }

    override fun deleteTag(id: String): ResponseEntity<Unit> {
        tagService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
