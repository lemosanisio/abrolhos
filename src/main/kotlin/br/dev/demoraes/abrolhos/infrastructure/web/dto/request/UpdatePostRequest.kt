package br.dev.demoraes.abrolhos.infrastructure.web.dto.request

import br.dev.demoraes.abrolhos.domain.entities.CategoryName
import br.dev.demoraes.abrolhos.domain.entities.PostContent
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostTitle
import br.dev.demoraes.abrolhos.domain.entities.TagName

/**
 * Request body for updating an existing post (PUT /api/posts/{slug}).
 *
 * All fields are optional — only the non-null fields will be applied to the post.
 */
data class UpdatePostRequest(
    val title: PostTitle?,
    val content: PostContent?,
    val status: PostStatus?,
    val categoryName: CategoryName?,
    val tagNames: List<TagName>?,
)
