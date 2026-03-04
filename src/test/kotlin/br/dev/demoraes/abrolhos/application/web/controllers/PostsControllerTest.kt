package br.dev.demoraes.abrolhos.application.web.controllers

import br.dev.demoraes.abrolhos.application.services.PostService
import br.dev.demoraes.abrolhos.domain.entities.Category
import br.dev.demoraes.abrolhos.domain.entities.CategoryName
import br.dev.demoraes.abrolhos.domain.entities.CategorySlug
import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.PostContent
import br.dev.demoraes.abrolhos.domain.entities.PostSlug
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostSummary
import br.dev.demoraes.abrolhos.domain.entities.PostTitle
import br.dev.demoraes.abrolhos.domain.entities.Role
import br.dev.demoraes.abrolhos.domain.entities.TagName
import br.dev.demoraes.abrolhos.domain.entities.TotpSecret
import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import br.dev.demoraes.abrolhos.infrastructure.web.config.SecurityConfig
import br.dev.demoraes.abrolhos.infrastructure.web.controllers.PostsController
import br.dev.demoraes.abrolhos.infrastructure.web.dto.request.CreatePostRequest
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import ulid.ULID
import java.time.OffsetDateTime

@WebMvcTest(PostsController::class)
@Import(
    SecurityConfig::class,
    br.dev.demoraes.abrolhos.infrastructure.web.config.TestConfig::class,
    PostsControllerTest.TestSecurityConfig::class
)
class PostsControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockkBean private lateinit var postService: PostService

    @Suppress("UnusedPrivateProperty")
    @MockkBean
    private lateinit var userRepository: UserRepository

    @Suppress("UnusedPrivateProperty")
    @MockkBean
    private lateinit var encryptionService:
        br.dev.demoraes.abrolhos.application.services.EncryptionService

    @Suppress("UnusedPrivateProperty")
    @MockkBean
    private lateinit var rateLimitService:
        br.dev.demoraes.abrolhos.application.services.RateLimitService

    @Suppress("UnusedPrivateProperty")
    @MockkBean
    private lateinit var auditLogger: br.dev.demoraes.abrolhos.application.audit.AuditLogger

    @Autowired private lateinit var objectMapper: ObjectMapper

    @org.springframework.boot.test.context.TestConfiguration
    class TestSecurityConfig {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        fun corsConfig(): br.dev.demoraes.abrolhos.infrastructure.web.config.CorsConfig {
            val mock =
                mockk<
                    br.dev.demoraes.abrolhos.infrastructure.web.config.CorsConfig
                    >(
                    relaxed = true
                )
            every { mock.corsConfigurationSource() } returns mockk(relaxed = true)
            return mock
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun makePost(slug: String = "post-slug"): Post {
        val author =
            User(
                id = ULID.nextULID(),
                username = Username("author"),
                totpSecret = TotpSecret("JBSWY3DPEHPK3PXP"),
                passwordHash = null,
                isActive = true,
                role = Role.USER,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )
        val category =
            Category(
                id = ULID.nextULID(),
                name = CategoryName("Category"),
                slug = CategorySlug("category"),
                posts = emptySet(),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )
        return Post(
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
            updatedAt = OffsetDateTime.now(),
        )
    }

    // -------------------------------------------------------------------------
    // GET /api/posts
    // -------------------------------------------------------------------------

    @Test
    fun `should search posts successfully`() {
        val summary =
            object : PostSummary {
                override val id = ULID.nextULID().toString()
                override val authorUsername = "author"
                override val title = "Post Title"
                override val slug = "post-slug"
                override val categoryName = "Category"
                override val shortContent = "Short content"
                override val publishedAt = OffsetDateTime.now()
            }
        val page: org.springframework.data.domain.Page<PostSummary> =
            PageImpl(listOf(summary))
        every { postService.searchPostSummaries(any(), any(), any(), any()) } returns page

        mockMvc.perform(
            get("/api/posts")
                .param("page", "0")
                .param("size", "10")
                .param("status", "PUBLISHED")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].title").value("Post Title"))
    }

    // -------------------------------------------------------------------------
    // GET /api/posts/{slug}
    // -------------------------------------------------------------------------

    @Test
    fun `should get published post by slug without auth`() {
        val post = makePost()
        every { postService.findBySlugForUser("post-slug", null, null) } returns post

        mockMvc.perform(get("/api/posts/post-slug"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Post Title"))
    }

    @Test
    @WithMockUser(username = "author", roles = ["USER"])
    fun `should get draft post by slug as owner`() {
        val post = makePost().copy(status = PostStatus.DRAFT, publishedAt = null)
        every { postService.findBySlugForUser(eq("post-slug"), any(), any()) } returns post

        mockMvc.perform(get("/api/posts/post-slug")).andExpect(status().isOk)
    }

    // -------------------------------------------------------------------------
    // POST /api/posts
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "author")
    fun `should create post successfully`() {
        val request =
            CreatePostRequest(
                title = PostTitle("New Post"),
                content = PostContent("New content"),
                status = PostStatus.PUBLISHED,
                categoryName = CategoryName("Category"),
                tagNames = listOf(TagName("Tag")),
                authorUsername = Username("author"),
            )
        val post = makePost("new-post").copy(title = PostTitle("New Post"))
        every { postService.createPost(any(), any(), any(), any(), any(), any()) } returns
            post

        mockMvc.perform(
            post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.slug").value("new-post"))
    }

    // -------------------------------------------------------------------------
    // PUT /api/posts/{slug}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "author", roles = ["USER"])
    fun `should update post successfully as owner`() {
        val updatedPost = makePost()
        every {
            postService.updatePost(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns updatedPost

        val body = mapOf("content" to "Updated content")
        mockMvc.perform(
            put("/api/posts/post-slug")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `should return 403 for PUT without authentication`() {
        mockMvc.perform(
            put("/api/posts/post-slug")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "other", roles = ["USER"])
    fun `should return 403 for PUT when not post owner`() {
        every {
            postService.updatePost(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } throws AccessDeniedException("Access denied")

        mockMvc.perform(
            put("/api/posts/post-slug")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `should update post successfully as admin`() {
        val updatedPost = makePost()
        every {
            postService.updatePost(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns updatedPost

        mockMvc.perform(
            put("/api/posts/post-slug")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isOk)
    }

    // -------------------------------------------------------------------------
    // DELETE /api/posts/{slug}
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(username = "author", roles = ["USER"])
    fun `should delete post successfully as owner`() {
        every { postService.deletePost(any(), any(), any()) } returns Unit

        mockMvc.perform(delete("/api/posts/post-slug")).andExpect(status().isNoContent)

        verify { postService.deletePost("post-slug", "author", any()) }
    }

    @Test
    fun `should return 403 for DELETE without authentication`() {
        mockMvc.perform(delete("/api/posts/post-slug")).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "other", roles = ["USER"])
    fun `should return 403 for DELETE when not post owner`() {
        every { postService.deletePost(any(), any(), any()) } throws
            AccessDeniedException("Access denied")

        mockMvc.perform(delete("/api/posts/post-slug")).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN"])
    fun `should delete post successfully as admin`() {
        every { postService.deletePost(any(), any(), any()) } returns Unit

        mockMvc.perform(delete("/api/posts/post-slug")).andExpect(status().isNoContent)
    }
}
