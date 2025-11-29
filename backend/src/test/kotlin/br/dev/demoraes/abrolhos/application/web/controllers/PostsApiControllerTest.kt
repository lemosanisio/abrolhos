package br.dev.demoraes.abrolhos.application.web.controllers

import br.dev.demoraes.abrolhos.domain.entities.Category
import br.dev.demoraes.abrolhos.domain.entities.CategoryName
import br.dev.demoraes.abrolhos.domain.entities.CategorySlug
import br.dev.demoraes.abrolhos.domain.entities.Email
import br.dev.demoraes.abrolhos.domain.entities.PasswordHash
import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.PostContent
import br.dev.demoraes.abrolhos.domain.entities.PostSlug
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostTitle
import br.dev.demoraes.abrolhos.domain.entities.Role
import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.services.PostService
import java.time.OffsetDateTime
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.thymeleaf.spring6.SpringTemplateEngine
import ulid.ULID

@WebMvcTest(PostsApiController::class)
@AutoConfigureMockMvc(addFilters = false)
class PostsApiControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockBean private lateinit var postService: PostService

    @MockBean private lateinit var templateEngine: SpringTemplateEngine

    @Test
    fun `should return HTMX view for published posts list`() {
        `when`(postService.listPublishedPosts()).thenReturn(emptySet())
        `when`(templateEngine.process(eq("posts/list"), any())).thenReturn("<div>list</div>")

        mockMvc.perform(get("/api/v1/posts").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk)
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("<div>list</div>"))

        verify(templateEngine).process(eq("posts/list"), any())
    }

    @Test
    fun `should return HTMX view for post detail`() {
        val slug = "test-post"
        val post =
                Post(
                        id = ULID.nextULID(),
                        author =
                                User(
                                        id = ULID.nextULID(),
                                        username = Username("author"),
                                        email = Email("author@example.com"),
                                        passwordHash = PasswordHash("hash"),
                                        role = Role.USER,
                                        createdAt = OffsetDateTime.now(),
                                        updatedAt = OffsetDateTime.now()
                                ),
                        title = PostTitle("Test Post"),
                        slug = PostSlug(slug),
                        content = PostContent("Content"),
                        status = PostStatus.PUBLISHED,
                        category =
                                Category(
                                        id = ULID.nextULID(),
                                        name = CategoryName("Tech"),
                                        slug = CategorySlug("tech"),
                                        posts = emptySet(),
                                        createdAt = OffsetDateTime.now(),
                                        updatedAt = OffsetDateTime.now()
                                ),
                        tags = emptySet(),
                        createdAt = OffsetDateTime.now(),
                        updatedAt = OffsetDateTime.now(),
                        publishedAt = OffsetDateTime.now()
                )

        `when`(postService.getPublishedPostBySlug(slug)).thenReturn(post)
        `when`(templateEngine.process(eq("fragments/post-detail"), any()))
                .thenReturn("<div>detail</div>")

        mockMvc.perform(get("/api/v1/posts/$slug").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk)
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("<div>detail</div>"))

        verify(templateEngine).process(eq("fragments/post-detail"), any())
    }

    @Test
    fun `should return 404 view when post not found for HTMX`() {
        val slug = "unknown"
        `when`(postService.getPublishedPostBySlug(slug))
                .thenThrow(NoSuchElementException::class.java)
        `when`(templateEngine.process(eq("error/404"), any())).thenReturn("<div>404</div>")

        // Note: The controller catches NoSuchElementException and returns "error/404" view
        mockMvc.perform(get("/api/v1/posts/$slug").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound)
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("<div>404</div>"))

        verify(templateEngine).process(eq("error/404"), any())
    }
}
