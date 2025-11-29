package br.dev.demoraes.abrolhos.application.web.controllers

import br.dev.demoraes.abrolhos.application.dto.CategoryRequest
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
import br.dev.demoraes.abrolhos.domain.services.CategoryService
import br.dev.demoraes.abrolhos.domain.services.PostService
import br.dev.demoraes.abrolhos.domain.services.commands.CreateCategoryCommand
import br.dev.demoraes.abrolhos.domain.services.commands.UpdateCategoryCommand
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

@WebMvcTest(CategoriesApiController::class)
@AutoConfigureMockMvc(addFilters = false)
class CategoriesApiControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockBean private lateinit var categoryService: CategoryService

    @MockBean private lateinit var postService: PostService

    @Autowired private lateinit var objectMapper: ObjectMapper

    private val categoryId = ULID.nextULID()
    private val categoryName = "Technology"
    private val categorySlug = "technology"
    private val category =
            Category(
                    id = categoryId,
                    name = CategoryName(categoryName),
                    slug = CategorySlug(categorySlug),
                    posts = emptySet(),
                    createdAt = OffsetDateTime.now(),
                    updatedAt = OffsetDateTime.now()
            )

    @Test
    fun `should list categories`() {
        `when`(categoryService.listCategories()).thenReturn(listOf(category))

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0].id").value(categoryId.toString()))
                .andExpect(jsonPath("$[0].name").value(categoryName))
                .andExpect(jsonPath("$[0].slug").value(categorySlug))
    }

    @Test
    fun `should get category by slug`() {
        `when`(categoryService.getBySlug(categorySlug)).thenReturn(category)

        mockMvc.perform(get("/api/v1/categories/{slug}", categorySlug))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.name").value(categoryName))
                .andExpect(jsonPath("$.slug").value(categorySlug))
    }

    @Test
    fun `should return 404 when category not found by slug`() {
        `when`(categoryService.getBySlug("unknown")).thenReturn(null)

        mockMvc.perform(get("/api/v1/categories/{slug}", "unknown")).andExpect(status().isNotFound)
    }

    @Test
    fun `should create category`() {
        val request = CategoryRequest(name = categoryName)
        `when`(categoryService.create(safeEq(CreateCategoryCommand(name = categoryName))))
                .thenReturn(category)

        mockMvc.perform(
                        post("/api/v1/admin/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").value(categoryId.toString()))
                .andExpect(jsonPath("$.name").value(categoryName))
    }

    @Test
    fun `should update category`() {
        val request = CategoryRequest(name = "New Name")
        val updatedCategory = category.copy(name = CategoryName("New Name"))
        `when`(
                        categoryService.update(
                                safeEq(
                                        UpdateCategoryCommand(
                                                id = categoryId.toString(),
                                                name = "New Name"
                                        )
                                )
                        )
                )
                .thenReturn(updatedCategory)

        mockMvc.perform(
                        put("/api/v1/admin/categories/{id}", categoryId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value("New Name"))
    }

    @Test
    fun `should delete category`() {
        doNothing().`when`(categoryService).delete(categoryId.toString())

        mockMvc.perform(delete("/api/v1/admin/categories/{id}", categoryId.toString()))
                .andExpect(status().isNoContent)
    }

    @Test
    fun `should get posts by category`() {
        val categorySlug = "tech"
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

        `when`(postService.getPublishedPostsByCategory(categorySlug)).thenReturn(setOf(post))

        mockMvc.perform(get("/api/v1/categories/$categorySlug/posts"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$[0].title").value("Post Title"))
                .andExpect(jsonPath("$[0].slug").value("post-slug"))
    }

    @Test
    fun `should return 404 when category not found for posts`() {
        val categorySlug = "unknown"
        `when`(postService.getPublishedPostsByCategory(categorySlug))
                .thenThrow(NoSuchElementException::class.java)

        mockMvc.perform(get("/api/v1/categories/$categorySlug/posts"))
                .andExpect(status().isNotFound)
    }

    @MockBean private lateinit var templateEngine: SpringTemplateEngine

    @Test
    fun `should return HTMX view for category list`() {
        `when`(categoryService.listCategories()).thenReturn(emptyList())
        `when`(templateEngine.process(eq("categories/list"), any())).thenReturn("<div>list</div>")

        mockMvc.perform(get("/api/v1/categories").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk)
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("<div>list</div>"))

        verify(templateEngine).process(eq("categories/list"), any())
    }

    @Test
    fun `should return HTMX view for category detail`() {
        val categorySlug = "tech"
        val category =
                Category(
                        id = ULID.nextULID(),
                        name = CategoryName("Tech"),
                        slug = CategorySlug("tech"),
                        posts = emptySet(),
                        createdAt = OffsetDateTime.now(),
                        updatedAt = OffsetDateTime.now(),
                )

        `when`(categoryService.getBySlug(categorySlug)).thenReturn(category)
        `when`(templateEngine.process(eq("categories/detail"), any()))
                .thenReturn("<div>detail</div>")

        mockMvc.perform(get("/api/v1/categories/$categorySlug").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk)
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("<div>detail</div>"))

        verify(templateEngine).process(eq("categories/detail"), any())
    }

    @Test
    fun `should return HTMX view for category posts`() {
        val categorySlug = "tech"
        `when`(postService.getPublishedPostsByCategory(categorySlug)).thenReturn(emptySet())
        `when`(templateEngine.process(eq("categories/posts"), any())).thenReturn("<div>posts</div>")

        mockMvc.perform(get("/api/v1/categories/$categorySlug/posts").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk)
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("<div>posts</div>"))

        verify(templateEngine).process(eq("categories/posts"), any())
    }

    private fun <T> safeEq(value: T): T {
        eq(value)
        return value
    }
}
