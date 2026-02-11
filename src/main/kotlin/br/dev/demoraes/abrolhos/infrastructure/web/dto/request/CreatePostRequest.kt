package br.dev.demoraes.abrolhos.infrastructure.web.dto.request

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
    val categoryName: CategoryName,
    val tagNames: List<TagName>,
    val createdAt: OffsetDateTime? = null,
    val authorUsername: Username
)
