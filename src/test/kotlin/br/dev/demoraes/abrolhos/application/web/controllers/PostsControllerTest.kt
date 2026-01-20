package br.dev.demoraes.abrolhos.application.web.controllers

import br.dev.demoraes.abrolhos.application.config.SecurityConfig
import br.dev.demoraes.abrolhos.infrastructure.web.dto.request.CategoryRequest
import br.dev.demoraes.abrolhos.infrastructure.web.dto.request.CreatePostRequest
import br.dev.demoraes.abrolhos.infrastructure.web.dto.request.TagRequest
import br.dev.demoraes.abrolhos.domain.entities.Category
import br.dev.demoraes.abrolhos.domain.entities.CategoryName
import br.dev.demoraes.abrolhos.domain.entities.CategorySlug
import br.dev.demoraes.abrolhos.domain.entities.Email
import br.dev.demoraes.abrolhos.domain.entities.PasswordHash
import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.PostContent
import br.dev.demoraes.abrolhos.domain.entities.PostSlug
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostSummary
import br.dev.demoraes.abrolhos.domain.entities.PostTitle
import br.dev.demoraes.abrolhos.domain.entities.Role
import br.dev.demoraes.abrolhos.domain.entities.TagName
import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.application.services.PostService
import br.dev.demoraes.abrolhos.infrastructure.web.controllers.PostsController
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ulid.ULID
import java.time.OffsetDateTime

@WebMvcTest(PostsController::class)
@Import(SecurityConfig::class)
class PostsControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var postService: PostService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should search posts successfully`() {
        // Given
        val author = User(
            id = ULID.nextULID(),
            username = Username("author"),
            email = Email("author@example.com"),
            passwordHash = PasswordHash("hash"),
            role = Role.USER,
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )
        val category = Category(
            id = ULID.nextULID(),
            name = CategoryName("Category"),
            slug = CategorySlug("category"),
            posts = emptySet(),
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )
        val summary = object : PostSummary {
            override val id = ULID.nextULID().toString()
            override val authorUsername = "author"
            override val title = "Post Title"
            override val slug = "post-slug"
            override val categoryName = "Category"
            override val shortContent = "Short content"
            override val publishedAt = OffsetDateTime.now()
        }
        val page: org.springframework.data.domain.Page<PostSummary> = PageImpl(listOf(summary))

        every { postService.searchPostSummaries(any(), any(), any(), any()) } returns page

        // When / Then
        mockMvc.perform(
            get("/api/posts")
                .param("page", "0")
                .param("size", "10")
                .param("status", "PUBLISHED")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].title").value("Post Title"))
            .andExpect(jsonPath("$.content[0].slug").value("post-slug"))
    }

    @Test
    fun `should get post by slug successfully`() {
        // Given
        val slug = "post-slug"
        val author = User(
            id = ULID.nextULID(),
            username = Username("author"),
            email = Email("author@example.com"),
            passwordHash = PasswordHash("hash"),
            role = Role.USER,
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )
        val category = Category(
            id = ULID.nextULID(),
            name = CategoryName("Category"),
            slug = CategorySlug("category"),
            posts = emptySet(),
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )
        val post = Post(
            id = ULID.nextULID(),
            author = author,
            title = PostTitle("Post Title"),
            slug = PostSlug(slug),
            content = PostContent("Full content"),
            status = PostStatus.PUBLISHED,
            category = category,
            tags = emptySet(),
            publishedAt = OffsetDateTime.now(),
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )

        every { postService.findBySlug(slug) } returns post

        // When / Then
        mockMvc.perform(get("/api/posts/$slug"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Post Title"))
            .andExpect(jsonPath("$.slug").value(slug))
            .andExpect(jsonPath("$.content").value("Full content"))
    }

    @Test
    fun `should create post successfully`() {
        // Given
        val request = CreatePostRequest(
            title = PostTitle("New Post"),
            content = PostContent("New content"),
            status = PostStatus.PUBLISHED,
            categoryName = CategoryRequest(CategoryName("Category")),
            tagNames = listOf(TagRequest(TagName("Tag"))),
            authorUsername = Username("author")
        )

        val author = User(
            id = ULID.nextULID(),
            username = Username("author"),
            email = Email("author@example.com"),
            passwordHash = PasswordHash("hash"),
            role = Role.USER,
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )
        val category = Category(
            id = ULID.nextULID(),
            name = CategoryName("Category"),
            slug = CategorySlug("category"),
            posts = emptySet(),
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )
        val post = Post(
            id = ULID.nextULID(),
            author = author,
            title = PostTitle("New Post"),
            slug = PostSlug("new-post"),
            content = PostContent("New content"),
            status = PostStatus.PUBLISHED,
            category = category,
            tags = emptySet(),
            publishedAt = OffsetDateTime.now(),
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )

        every { postService.createPost(any(), any(), any(), any(), any(), any()) } returns post

        // When / Then
        mockMvc.perform(
            post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("New Post"))
            .andExpect(jsonPath("$.slug").value("new-post"))
    }
}
