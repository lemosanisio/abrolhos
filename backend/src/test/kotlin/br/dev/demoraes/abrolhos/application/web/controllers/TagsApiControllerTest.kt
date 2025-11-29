package br.dev.demoraes.abrolhos.application.web.controllers

import br.dev.demoraes.abrolhos.application.dto.TagRequest
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
import br.dev.demoraes.abrolhos.domain.entities.Tag
import br.dev.demoraes.abrolhos.domain.entities.TagName
import br.dev.demoraes.abrolhos.domain.entities.TagSlug
import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.services.PostService
import br.dev.demoraes.abrolhos.domain.services.TagService
import br.dev.demoraes.abrolhos.domain.services.commands.CreateTagCommand
import br.dev.demoraes.abrolhos.domain.services.commands.UpdateTagCommand
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.OffsetDateTime
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.thymeleaf.spring6.SpringTemplateEngine
import ulid.ULID

@WebMvcTest(TagsApiController::class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for simplicity
class TagsApiControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockBean private lateinit var tagService: TagService

    @MockBean private lateinit var postService: PostService

    @Autowired private lateinit var objectMapper: ObjectMapper

    private val tagId = ULID.nextULID()
    private val tagName = "Technology"
    private val tagSlug = "technology"
    private val tag =
            Tag(
                    id = tagId,
                    name = TagName(tagName),
                    slug = TagSlug(tagSlug),
                    posts = emptySet(),
                    createdAt = OffsetDateTime.now(),
                    updatedAt = OffsetDateTime.now()
            )

    @Test
    fun `should list tags`() {
        `when`(tagService.listTags()).thenReturn(listOf(tag))

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0].id").value(tagId.toString()))
                .andExpect(jsonPath("$[0].name").value(tagName))
                .andExpect(jsonPath("$[0].slug").value(tagSlug))
    }

    @Test
    fun `should get tag by slug`() {
        `when`(tagService.getBySlug(tagSlug)).thenReturn(tag)

        mockMvc.perform(get("/api/v1/tags/{slug}", tagSlug))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(tagId.toString()))
                .andExpect(jsonPath("$.name").value(tagName))
                .andExpect(jsonPath("$.slug").value(tagSlug))
    }

    @Test
    fun `should return 404 when tag not found by slug`() {
        `when`(tagService.getBySlug("unknown")).thenReturn(null)

        mockMvc.perform(get("/api/v1/tags/{slug}", "unknown")).andExpect(status().isNotFound)
    }

    @Test
    fun `should create tag`() {
        val request = TagRequest(name = tagName)
        `when`(tagService.create(safeEq(CreateTagCommand(name = tagName)))).thenReturn(tag)

        mockMvc.perform(
                        post("/api/v1/admin/tags")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").value(tagId.toString()))
                .andExpect(jsonPath("$.name").value(tagName))
    }

    @Test
    fun `should update tag`() {
        val request = TagRequest(name = "NewName")
        val updatedTag = tag.copy(name = TagName("NewName"))
        `when`(tagService.update(safeEq(UpdateTagCommand(id = tagId.toString(), name = "NewName"))))
                .thenReturn(updatedTag)

        mockMvc.perform(
                        put("/api/v1/admin/tags/{id}", tagId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value("NewName"))
    }

    @Test
    fun `should delete tag`() {
        doNothing().`when`(tagService).delete(tagId.toString())

        mockMvc.perform(delete("/api/v1/admin/tags/{id}", tagId.toString()))
                .andExpect(status().isNoContent)
    }

    @Test
    fun `should get posts by tag`() {
        val tagSlug = "tech"
        val post =
                Post(
                        id = ULID.nextULID(),
                        author =
                                User(
                                        id = ULID.nextULID(),
                                        username = Username("author"),
                                        email = Email("author@example.com"),
                                        passwordHash = PasswordHash("hashed_password"),
                                        role = Role.USER,
                                        createdAt = OffsetDateTime.now(),
                                        updatedAt = OffsetDateTime.now(),
                                ),
                        title = PostTitle("Post Title"),
                        slug = PostSlug("post-slug"),
                        content = PostContent("Content"),
                        status = PostStatus.PUBLISHED,
                        category =
                                Category(
                                        id = ULID.nextULID(),
                                        name = CategoryName("Tech"),
                                        slug = CategorySlug("tech"),
                                        posts = emptySet(),
                                        createdAt = OffsetDateTime.now(),
                                        updatedAt = OffsetDateTime.now(),
                                ),
                        tags = emptySet(),
                        createdAt = OffsetDateTime.now(),
                        updatedAt = OffsetDateTime.now(),
                        publishedAt = OffsetDateTime.now(),
                )

        `when`(postService.getPublishedPostsByTag(tagSlug)).thenReturn(setOf(post))

        mockMvc.perform(get("/api/v1/tags/$tagSlug/posts"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0].title").value("Post Title"))
                .andExpect(jsonPath("$[0].slug").value("post-slug"))
    }

    @Test
    fun `should return 404 when tag not found for posts`() {
        val tagSlug = "unknown"
        `when`(postService.getPublishedPostsByTag(tagSlug))
                .thenThrow(NoSuchElementException::class.java)

        mockMvc.perform(get("/api/v1/tags/$tagSlug/posts")).andExpect(status().isNotFound)
    }

    @MockBean private lateinit var templateEngine: SpringTemplateEngine

    @Test
    fun `should return HTMX view for tag list`() {
        `when`(tagService.listTags()).thenReturn(emptyList())
        `when`(templateEngine.process(eq("tags/list"), any())).thenReturn("<div>list</div>")

        mockMvc.perform(get("/api/v1/tags").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk)
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("<div>list</div>"))

        verify(templateEngine).process(eq("tags/list"), any())
    }

    @Test
    fun `should return HTMX view for tag detail`() {
        val tagSlug = "tech"
        val tag =
                Tag(
                        id = ULID.nextULID(),
                        name = TagName("Tech"),
                        slug = TagSlug("tech"),
                        posts = emptySet(),
                        createdAt = OffsetDateTime.now(),
                        updatedAt = OffsetDateTime.now(),
                )

        `when`(tagService.getBySlug(tagSlug)).thenReturn(tag)
        `when`(templateEngine.process(eq("tags/detail"), any())).thenReturn("<div>detail</div>")

        mockMvc.perform(get("/api/v1/tags/$tagSlug").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk)
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("<div>detail</div>"))

        verify(templateEngine).process(eq("tags/detail"), any())
    }

    @Test
    fun `should return HTMX view for tag posts`() {
        val tagSlug = "tech"
        `when`(postService.getPublishedPostsByTag(tagSlug)).thenReturn(emptySet())
        `when`(templateEngine.process(eq("tags/posts"), any())).thenReturn("<div>posts</div>")

        mockMvc.perform(get("/api/v1/tags/$tagSlug/posts").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk)
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("<div>posts</div>"))

        verify(templateEngine).process(eq("tags/posts"), any())
    }

    private fun <T> safeEq(value: T): T {
        eq(value)
        return value
    }
}
