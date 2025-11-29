package br.dev.demoraes.abrolhos.application.web.dto.request

import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import java.time.OffsetDateTime

data class PostCreateRequest(
    val title: String,
    val content: String,
    val status: PostStatus,
    val category: CategoryRequest,
    val tags: List<TagRequest>,
    val createdAt: OffsetDateTime? = null
)

data class CategoryRequest(
    val name: String
)

data class TagRequest(
    val name: String
)
