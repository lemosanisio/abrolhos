package br.dev.demoraes.abrolhos.application.web.controllers

import br.dev.demoraes.abrolhos.application.api.CategoriesApi
import br.dev.demoraes.abrolhos.application.dto.CategoryRequest
import br.dev.demoraes.abrolhos.application.dto.CategoryResponse
import br.dev.demoraes.abrolhos.application.dto.PostResponse
import br.dev.demoraes.abrolhos.application.web.dto.mappers.DtoMappers.toResponse
import br.dev.demoraes.abrolhos.domain.entities.toGeneratedResponse
import br.dev.demoraes.abrolhos.domain.services.CategoryService
import br.dev.demoraes.abrolhos.domain.services.PostService
import br.dev.demoraes.abrolhos.domain.services.commands.CreateCategoryCommand
import br.dev.demoraes.abrolhos.domain.services.commands.UpdateCategoryCommand
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
class CategoriesApiController(
    private val categoryService: CategoryService,
    private val postService: PostService,
    private val templateEngine: TemplateEngine,
) : CategoriesApi {
    override fun listCategories(): ResponseEntity<List<CategoryResponse>> =
        ResponseEntity.ok(categoryService.listCategories().map { it.toResponse() })

    override fun getCategoryBySlug(slug: String): ResponseEntity<CategoryResponse> {
        val category = categoryService.getBySlug(slug) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(category.toResponse())
    }

    override fun getPostsByCategory(slug: String): ResponseEntity<List<PostResponse>> {
        val posts = postService.getPublishedPostsByCategory(slug)
        return ResponseEntity.ok(posts.map { it.toGeneratedResponse() })
    }

    // HTMX Methods

    @GetMapping(value = ["/categories"], produces = [MediaType.TEXT_HTML_VALUE])
    fun listCategoriesHtmx(): ResponseEntity<String> {
        val categories = categoryService.listCategories().map { it.toResponse() }
        val context = Context()
        context.setVariable("categories", categories)
        val html = templateEngine.process("categories/list", context)
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html)
    }

    @GetMapping(value = ["/categories/{slug}"], produces = [MediaType.TEXT_HTML_VALUE])
    fun getCategoryHtmx(
        @PathVariable slug: String,
        @org.springframework.web.bind.annotation.RequestHeader(
            value = "HX-Request",
            required = false
        )
        hxRequest: String?
    ): ResponseEntity<String> {
        val category = categoryService.getBySlug(slug)?.toResponse()
        return if (category != null) {
            val context = Context()
            context.setVariable("category", category)
            val viewName =
                if (hxRequest == "true") "fragments/category-detail" else "categories/detail"
            val html = templateEngine.process(viewName, context)
            ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html)
        } else {
            val html = templateEngine.process("error/404", Context())
            ResponseEntity.status(404).contentType(MediaType.TEXT_HTML).body(html)
        }
    }

    @GetMapping(value = ["/categories/{slug}/posts"], produces = [MediaType.TEXT_HTML_VALUE])
    fun getPostsByCategoryHtmx(
        @PathVariable slug: String,
        @org.springframework.web.bind.annotation.RequestHeader(
            value = "HX-Request",
            required = false
        )
        hxRequest: String?
    ): ResponseEntity<String> {
        val posts = postService.getPublishedPostsByCategory(slug).map { it.toGeneratedResponse() }
        val context = Context()
        context.setVariable("posts", posts)
        context.setVariable("slug", slug)
        val viewName = if (hxRequest == "true") "fragments/category-posts" else "categories/posts"
        val html = templateEngine.process(viewName, context)
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html)
    }

    override fun createCategory(
        categoryRequest: CategoryRequest
    ): ResponseEntity<CategoryResponse> {
        val command = CreateCategoryCommand(name = categoryRequest.name)
        val created = categoryService.create(command)
        val response = created.toResponse()
        return ResponseEntity.created(URI.create("/api/v1/categories/${response.slug}"))
            .body(response)
    }

    override fun updateCategory(
        id: String,
        categoryRequest: CategoryRequest,
    ): ResponseEntity<CategoryResponse> {
        val command = UpdateCategoryCommand(id = id, name = categoryRequest.name)
        val updated = categoryService.update(command)
        return ResponseEntity.ok(updated.toResponse())
    }

    override fun deleteCategory(id: String): ResponseEntity<Unit> {
        categoryService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
