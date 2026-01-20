package br.dev.demoraes.abrolhos.domain.services

import br.dev.demoraes.abrolhos.application.services.PostService
import br.dev.demoraes.abrolhos.domain.entities.Category
import br.dev.demoraes.abrolhos.domain.entities.CategoryName
import br.dev.demoraes.abrolhos.domain.entities.CategorySlug
import br.dev.demoraes.abrolhos.domain.entities.Email
import br.dev.demoraes.abrolhos.domain.entities.PasswordHash
import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostSummary
import br.dev.demoraes.abrolhos.domain.entities.Role
import br.dev.demoraes.abrolhos.domain.entities.Tag
import br.dev.demoraes.abrolhos.domain.entities.TagName
import br.dev.demoraes.abrolhos.domain.entities.TagSlug
import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.repository.CategoryRepository
import br.dev.demoraes.abrolhos.domain.repository.PostRepository
import br.dev.demoraes.abrolhos.domain.repository.TagRepository
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import ulid.ULID
import java.time.OffsetDateTime

class PostServiceTest {

    private val postRepository = mockk<PostRepository>()
    private val userRepository = mockk<UserRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val tagRepository = mockk<TagRepository>()

    private val postService = PostService(
        postRepository,
        userRepository,
        categoryRepository,
        tagRepository
    )

    @Test
    fun `createPost should create and save a post successfully`() {
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
        val tag = Tag(
            id = ULID.nextULID(),
            name = TagName("Tag"),
            slug = TagSlug("tag"),
            posts = emptySet(),
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )

        every { userRepository.findByUsername(Username("author")) } returns author
        every { categoryRepository.findByName(CategoryName("Category")) } returns category
        every { tagRepository.findByName(TagName("Tag")) } returns tag
        every { postRepository.save(any()) } answers { firstArg() }

        // When
        val post = postService.createPost(
            title = "Post Title",
            content = "Post Content",
            status = PostStatus.PUBLISHED,
            categoryName = "Category",
            tagNames = listOf("Tag"),
            authorUsername = "author"
        )

        // Then
        assertNotNull(post)
        assertEquals("Post Title", post.title.value)
        assertEquals("post-title", post.slug.value)
        assertEquals("Post Content", post.content.value)
        assertEquals(PostStatus.PUBLISHED, post.status)
        assertEquals(author, post.author)
        assertEquals(category, post.category)
        assertTrue(post.tags.contains(tag))
        assertNotNull(post.publishedAt)

        verify { postRepository.save(any()) }
    }

    @Test
    fun `createPost should generate correct slug with special characters`() {
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
        every { userRepository.findByUsername(Username("author")) } returns author
        every { categoryRepository.findByName(any()) } returns mockk(relaxed = true)
        every { tagRepository.findByName(any()) } returns mockk(relaxed = true)
        every { postRepository.save(any()) } answers { firstArg() }

        // When
        val post = postService.createPost(
            title = "  My Post Title!!!  123  ",
            content = "Content",
            status = PostStatus.DRAFT,
            categoryName = "Category",
            tagNames = emptyList(),
            authorUsername = "author"
        )

        // Then
        assertEquals("my-post-title-123", post.slug.value)
    }

    @Test
    fun `createPost should throw exception when author not found`() {
        // Given
        every { userRepository.findByUsername(Username("author")) } returns null

        // When / Then
        assertThrows<NoSuchElementException> {
            postService.createPost(
                title = "Post Title",
                content = "Post Content",
                status = PostStatus.PUBLISHED,
                categoryName = "Category",
                tagNames = listOf("Tag"),
                authorUsername = "author"
            )
        }
    }

    @Test
    fun `createPost should create category if not found`() {
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
        every { userRepository.findByUsername(Username("author")) } returns author
        every { categoryRepository.findByName(CategoryName("New Category")) } returns null
        every { categoryRepository.save(any()) } answers { firstArg() }
        every { tagRepository.findByName(TagName("New Tag")) } returns null
        every { tagRepository.save(any()) } answers { firstArg() }
        every { postRepository.save(any()) } answers { firstArg() }

        // When
        postService.createPost(
            title = "Post Title",
            content = "Post Content",
            status = PostStatus.PUBLISHED,
            categoryName = "New Category",
            tagNames = listOf("New Tag"),
            authorUsername = "author"
        )

        // Then
        verify { categoryRepository.save(any()) }
        verify { tagRepository.save(any()) }
        verify { postRepository.save(any()) }
    }

    @Test
    fun `findBySlug should return post when found`() {
        // Given
        val slug = "post-slug"
        val post = mockk<Post>()
        every { postRepository.findPublishedBySlug(slug) } returns post

        // When
        val result = postService.findBySlug(slug)

        // Then
        assertEquals(post, result)
    }

    @Test
    fun `findBySlug should throw exception when not found`() {
        // Given
        val slug = "non-existent"
        every { postRepository.findPublishedBySlug(slug) } returns null

        // When / Then
        assertThrows<NoSuchElementException> {
            postService.findBySlug(slug)
        }
    }

    @Test
    fun `searchPostsSummary should return page of summaries`() {
        // Given
        val pageable = mockk<Pageable>()
        val summary = mockk<PostSummary>()
        val page = PageImpl(listOf(summary))
        every { postRepository.searchSummary(pageable, any(), any(), any()) } returns page

        // When
        val result = postService.searchPostSummaries(pageable, "Category", "Tag", PostStatus.PUBLISHED)

        // Then
        assertEquals(1, result.content.size)
        assertEquals(summary, result.content[0])
    }
}
