package br.dev.demoraes.abrolhos.application.web.dto.mappers

import br.dev.demoraes.abrolhos.application.dto.CategoryResponse
import br.dev.demoraes.abrolhos.application.dto.TagResponse
import br.dev.demoraes.abrolhos.domain.entities.Category
import br.dev.demoraes.abrolhos.domain.entities.Tag

object DtoMappers {
    fun Category.toResponse() =
        CategoryResponse(
            id = id.toString(),
            name = name.value,
            slug = slug.value,
        )

    fun Tag.toResponse() =
        TagResponse(
            id = id.toString(),
            name = name.value,
            slug = slug.value,
        )
}
