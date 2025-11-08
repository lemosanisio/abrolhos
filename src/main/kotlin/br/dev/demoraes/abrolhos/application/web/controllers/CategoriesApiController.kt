package br.dev.demoraes.abrolhos.application.web.controllers

import br.dev.demoraes.abrolhos.Application.api.CategoriesApi
import br.dev.demoraes.abrolhos.Application.dto.CategoryResponse
import br.dev.demoraes.abrolhos.application.web.dto.mappers.DtoMappers.toResponse
import br.dev.demoraes.abrolhos.domain.services.CategoryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class CategoriesApiController(
    private val categoryService: CategoryService,
) : CategoriesApi {
    override fun listCategories(): ResponseEntity<List<CategoryResponse>> =
        ResponseEntity.ok(categoryService.listCategories().map { it.toResponse() })

    override fun getCategoryBySlug(slug: String): ResponseEntity<CategoryResponse> =
        categoryService.getBySlug(slug)?.let {
            ResponseEntity.ok(it.toResponse())
        } ?: ResponseEntity.notFound().build()
}
