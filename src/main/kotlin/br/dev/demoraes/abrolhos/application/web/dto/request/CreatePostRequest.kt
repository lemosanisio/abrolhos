package br.dev.demoraes.abrolhos.application.web.dto.request

import br.dev.demoraes.abrolhos.domain.entities.CategoryName
import br.dev.demoraes.abrolhos.domain.entities.PostContent
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostTitle
import br.dev.demoraes.abrolhos.domain.entities.TagName
import br.dev.demoraes.abrolhos.domain.entities.Username
import java.time.OffsetDateTime

data class CreatePostRequest(
    val title: PostTitle,
    val content: PostContent,
    val status: PostStatus,
    val categoryName: CategoryRequest,
    val tagNames: List<TagRequest>,
    val createdAt: OffsetDateTime? = null,
    val authorUsername: Username
)

data class CategoryRequest(
    val name: CategoryName
)

data class TagRequest(
    val name: TagName
)
