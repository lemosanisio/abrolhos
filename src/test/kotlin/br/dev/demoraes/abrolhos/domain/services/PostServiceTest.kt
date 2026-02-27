package br.dev.demoraes.abrolhos.domain.services

import br.dev.demoraes.abrolhos.application.audit.AuditLogger
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
import br.dev.demoraes.abrolhos.domain.entities.Tag
import br.dev.demoraes.abrolhos.domain.entities.TagName
import br.dev.demoraes.abrolhos.domain.entities.TagSlug
import br.dev.demoraes.abrolhos.domain.entities.TotpSecret
import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.repository.CategoryRepository
import br.dev.demoraes.abrolhos.domain.repository.PostRepository
import br.dev.demoraes.abrolhos.domain.repository.TagRepository
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import br.dev.demoraes.abrolhos.infrastructure.monitoring.MetricsService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.OffsetDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import ulid.ULID

class PostServiceTest {

    private val postRepository = mockk<PostRepository>()
    private val userRepository = mockk<UserRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private val tagRepository = mockk<TagRepository>()
    private val metricsService = mockk<MetricsService>(relaxed = true)
    private val auditLogger = mockk<AuditLogger>(relaxed = true)

    private val postService =
            PostService(
                    postRepository,
                    userRepository,
                    categoryRepository,
                    tagRepository,
                    metricsService,
                    auditLogger,
            )

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun makeUser(role: Role = Role.USER) =
            User(
                    id = ULID.nextULID(),
                    username = Username("author"),
                    totpSecret = TotpSecret("JBSWY3DPEHPK3PXP"),
                    passwordHash = null,
                    isActive = true,
                    role = role,
                    createdAt = OffsetDateTime.now(),
                    updatedAt = OffsetDateTime.now(),
            )

    private fun makeCategory() =
            Category(
                    id = ULID.nextULID(),
                    name = CategoryName("Category"),
                    slug = CategorySlug("category"),
                    posts = emptySet(),
                    createdAt = OffsetDateTime.now(),
                    updatedAt = OffsetDateTime.now(),
            )

    private fun makePost(
            author: User,
            slug: String = "post-title",
            status: PostStatus = PostStatus.PUBLISHED
    ) =
            Post(
                    id = ULID.nextULID(),
                    author = author,
                    title = PostTitle("Post Title"),
                    slug = PostSlug(slug),
                    content = PostContent("Post Content"),
                    status = status,
                    publishedAt =
                            if (status == PostStatus.PUBLISHED) OffsetDateTime.now() else null,
                    category = makeCategory(),
                    tags = emptySet(),
                    createdAt = OffsetDateTime.now(),
                    updatedAt = OffsetDateTime.now(),
            )

    // -------------------------------------------------------------------------
    // createPost
    // -------------------------------------------------------------------------

    @Test
    fun `createPost should create and save a post successfully`() {
        val author = makeUser()
        val category = makeCategory()
        val tag =
                Tag(
                        id = ULID.nextULID(),
                        name = TagName("Tag"),
                        slug = TagSlug("tag"),
                        posts = emptySet(),
                        createdAt = OffsetDateTime.now(),
                        updatedAt = OffsetDateTime.now(),
                )

        every { userRepository.findByUsername(Username("author")) } returns author
        every { categoryRepository.findByName(CategoryName("Category")) } returns category
        every { tagRepository.findByName(TagName("Tag")) } returns tag
        every { postRepository.save(any()) } answers { firstArg() }

        val post =
                postService.createPost(
                        title = "Post Title",
                        content = "Post Content",
                        status = PostStatus.PUBLISHED,
                        categoryName = "Category",
                        tagNames = listOf("Tag"),
                        authorUsername = "author",
                )

        assertNotNull(post)
        assertEquals("Post Title", post.title.value)
        assertEquals("post-title", post.slug.value)
        assertEquals(PostStatus.PUBLISHED, post.status)
        assertNotNull(post.publishedAt)
        verify { postRepository.save(any()) }
    }

    @Test
    fun `createPost should generate correct slug with special characters`() {
        val author = makeUser()
        every { userRepository.findByUsername(Username("author")) } returns author
        every { categoryRepository.findByName(any()) } returns mockk(relaxed = true)
        every { tagRepository.findByName(any()) } returns mockk(relaxed = true)
        every { postRepository.save(any()) } answers { firstArg() }

        val post =
                postService.createPost(
                        title = "  My Post Title!!!  123  ",
                        content = "Content",
                        status = PostStatus.DRAFT,
                        categoryName = "Category",
                        tagNames = emptyList(),
                        authorUsername = "author",
                )

        assertEquals("my-post-title-123", post.slug.value)
    }

    @Test
    fun `createPost should throw exception when author not found`() {
        every { userRepository.findByUsername(Username("author")) } returns null

        assertThrows<NoSuchElementException> {
            postService.createPost(
                    title = "Post Title",
                    content = "Post Content",
                    status = PostStatus.PUBLISHED,
                    categoryName = "Category",
                    tagNames = listOf("Tag"),
                    authorUsername = "author",
            )
        }
    }

    @Test
    fun `createPost should create category if not found`() {
        val author = makeUser()
        every { userRepository.findByUsername(Username("author")) } returns author
        every { categoryRepository.findByName(CategoryName("New Category")) } returns null
        every { categoryRepository.save(any()) } answers { firstArg() }
        every { tagRepository.findByName(TagName("New Tag")) } returns null
        every { tagRepository.save(any()) } answers { firstArg() }
        every { postRepository.save(any()) } answers { firstArg() }

        postService.createPost(
                title = "Post Title",
                content = "Post Content",
                status = PostStatus.PUBLISHED,
                categoryName = "New Category",
                tagNames = listOf("New Tag"),
                authorUsername = "author",
        )

        verify { categoryRepository.save(any()) }
        verify { tagRepository.save(any()) }
    }

    // -------------------------------------------------------------------------
    // findPublishedBySlug
    // -------------------------------------------------------------------------

    @Test
    fun `findPublishedBySlug should return post when found`() {
        val post = mockk<Post>()
        every { postRepository.findPublishedBySlug("post-slug") } returns post

        val result = postService.findPublishedBySlug("post-slug")
        assertEquals(post, result)
    }

    @Test
    fun `findPublishedBySlug should throw when not found`() {
        every { postRepository.findPublishedBySlug("no-such") } returns null
        assertThrows<NoSuchElementException> { postService.findPublishedBySlug("no-such") }
    }

    // -------------------------------------------------------------------------
    // findBySlugForUser
    // -------------------------------------------------------------------------

    @Test
    fun `findBySlugForUser should return published post to unauthenticated user`() {
        val author = makeUser()
        val post = makePost(author, status = PostStatus.PUBLISHED)
        every { postRepository.findBySlug("post-title") } returns post

        val result = postService.findBySlugForUser("post-title", null, null)
        assertEquals(post, result)
        verify { metricsService.recordPostView() }
    }

    @Test
    fun `findBySlugForUser should return unpublished post to its owner`() {
        val author = makeUser()
        val post = makePost(author, status = PostStatus.DRAFT)
        every { postRepository.findBySlug("post-title") } returns post
        every { userRepository.findByUsername(Username("author")) } returns author

        val result = postService.findBySlugForUser("post-title", "author", Role.USER)
        assertEquals(post, result)
    }

    @Test
    fun `findBySlugForUser should return unpublished post to admin`() {
        val owner = makeUser()
        val admin = makeUser(Role.ADMIN)
        val post = makePost(owner, status = PostStatus.DRAFT)
        every { postRepository.findBySlug("post-title") } returns post

        val result = postService.findBySlugForUser("post-title", admin.username.value, Role.ADMIN)
        assertEquals(post, result)
    }

    @Test
    fun `findBySlugForUser should reject unauthenticated access to draft with 404`() {
        val author = makeUser()
        val post = makePost(author, status = PostStatus.DRAFT)
        every { postRepository.findBySlug("post-title") } returns post

        val ex =
                assertThrows<NoSuchElementException> {
                    postService.findBySlugForUser("post-title", null, null)
                }
        assertEquals("Post not found", ex.message)
    }

    @Test
    fun `findBySlugForUser should reject non-owner authenticated user with 404`() {
        val owner = makeUser()
        val other =
                User(
                        id = ULID.nextULID(),
                        username = Username("other"),
                        totpSecret = TotpSecret("JBSWY3DPEHPK3PXP"),
                        passwordHash = null,
                        isActive = true,
                        role = Role.USER,
                        createdAt = OffsetDateTime.now(),
                        updatedAt = OffsetDateTime.now(),
                )
        val post = makePost(owner, status = PostStatus.DRAFT)
        every { postRepository.findBySlug("post-title") } returns post
        every { userRepository.findByUsername(Username("other")) } returns other

        val ex =
                assertThrows<NoSuchElementException> {
                    postService.findBySlugForUser("post-title", "other", Role.USER)
                }
        assertEquals("Post not found", ex.message)
    }

    // -------------------------------------------------------------------------
    // updatePost
    // -------------------------------------------------------------------------

    @Test
    fun `updatePost should update post fields and record metrics`() {
        val author = makeUser()
        val post = makePost(author, status = PostStatus.DRAFT)
        every { postRepository.findBySlug("post-title") } returns post
        every { userRepository.findByUsername(Username("author")) } returns author
        every { categoryRepository.findByName(any()) } returns makeCategory()
        every { postRepository.findBySlug(any()) } returns null andThen post
        every { postRepository.save(any()) } answers { firstArg() }

        // Second call in generateUniqueSlug returns null (no conflict)
        every { postRepository.findBySlug("post-title") } returns post
        every { postRepository.findBySlug("new-title") } returns null

        val result =
                postService.updatePost(
                        slug = "post-title",
                        title = "New Title",
                        content = "New Content",
                        status = PostStatus.PUBLISHED,
                        categoryName = null,
                        tagNames = null,
                        currentUsername = "author",
                        currentUserRole = Role.USER,
                )

        assertNotNull(result)
        verify { metricsService.recordPostUpdate() }
        verify { auditLogger.logPostUpdate(any(), any(), any(), any()) }
    }

    @Test
    fun `updatePost should deny non-owner non-admin with AccessDeniedException`() {
        val owner = makeUser()
        val other =
                User(
                        id = ULID.nextULID(),
                        username = Username("other"),
                        totpSecret = TotpSecret("JBSWY3DPEHPK3PXP"),
                        passwordHash = null,
                        isActive = true,
                        role = Role.USER,
                        createdAt = OffsetDateTime.now(),
                        updatedAt = OffsetDateTime.now(),
                )
        val post = makePost(owner)
        every { postRepository.findBySlug("post-title") } returns post
        every { userRepository.findByUsername(Username("other")) } returns other

        assertThrows<AccessDeniedException> {
            postService.updatePost(
                    slug = "post-title",
                    title = "X",
                    content = null,
                    status = null,
                    categoryName = null,
                    tagNames = null,
                    currentUsername = "other",
                    currentUserRole = Role.USER,
            )
        }
    }

    @Test
    fun `updatePost should allow admin to update any post`() {
        val owner = makeUser()
        val admin =
                User(
                        id = ULID.nextULID(),
                        username = Username("sysadmin"),
                        totpSecret = TotpSecret("JBSWY3DPEHPK3PXP"),
                        passwordHash = null,
                        isActive = true,
                        role = Role.ADMIN,
                        createdAt = OffsetDateTime.now(),
                        updatedAt = OffsetDateTime.now(),
                )
        val post = makePost(owner)
        every { postRepository.findBySlug("post-title") } returns post
        every { userRepository.findByUsername(Username("sysadmin")) } returns admin
        every { postRepository.save(any()) } answers { firstArg() }

        val result =
                postService.updatePost(
                        slug = "post-title",
                        title = null,
                        content = "Updated",
                        status = null,
                        categoryName = null,
                        tagNames = null,
                        currentUsername = "sysadmin",
                        currentUserRole = Role.ADMIN,
                )

        assertNotNull(result)
    }

    @Test
    fun `updatePost should throw when post not found`() {
        every { postRepository.findBySlug("missing") } returns null
        assertThrows<NoSuchElementException> {
            postService.updatePost(
                    slug = "missing",
                    title = null,
                    content = null,
                    status = null,
                    categoryName = null,
                    tagNames = null,
                    currentUsername = "author",
                    currentUserRole = Role.USER,
            )
        }
    }

    // -------------------------------------------------------------------------
    // deletePost
    // -------------------------------------------------------------------------

    @Test
    fun `deletePost should soft-delete post and record metrics`() {
        val author = makeUser()
        val post = makePost(author)
        every { postRepository.findBySlug("post-title") } returns post
        every { userRepository.findByUsername(Username("author")) } returns author
        every { postRepository.delete(post) } returns Unit

        postService.deletePost("post-title", "author", Role.USER)

        verify { postRepository.delete(post) }
        verify { metricsService.recordPostDeletion() }
        verify { auditLogger.logPostDeletion(any(), any(), any()) }
    }

    @Test
    fun `deletePost should deny non-owner with AccessDeniedException`() {
        val owner = makeUser()
        val other =
                User(
                        id = ULID.nextULID(),
                        username = Username("other"),
                        totpSecret = TotpSecret("JBSWY3DPEHPK3PXP"),
                        passwordHash = null,
                        isActive = true,
                        role = Role.USER,
                        createdAt = OffsetDateTime.now(),
                        updatedAt = OffsetDateTime.now(),
                )
        val post = makePost(owner)
        every { postRepository.findBySlug("post-title") } returns post
        every { userRepository.findByUsername(Username("other")) } returns other

        assertThrows<AccessDeniedException> {
            postService.deletePost("post-title", "other", Role.USER)
        }
    }

    @Test
    fun `deletePost should allow admin to delete any post`() {
        val owner = makeUser()
        val admin =
                User(
                        id = ULID.nextULID(),
                        username = Username("sysadmin"),
                        totpSecret = TotpSecret("JBSWY3DPEHPK3PXP"),
                        passwordHash = null,
                        isActive = true,
                        role = Role.ADMIN,
                        createdAt = OffsetDateTime.now(),
                        updatedAt = OffsetDateTime.now(),
                )
        val post = makePost(owner)
        every { postRepository.findBySlug("post-title") } returns post
        every { userRepository.findByUsername(Username("sysadmin")) } returns admin
        every { postRepository.delete(post) } returns Unit

        postService.deletePost("post-title", "sysadmin", Role.ADMIN)
        verify { postRepository.delete(post) }
    }

    // -------------------------------------------------------------------------
    // generateUniqueSlug
    // -------------------------------------------------------------------------

    @Test
    fun `generateUniqueSlug should return base slug when no conflict`() {
        every { postRepository.findBySlug("hello-world") } returns null
        val slug = postService.generateUniqueSlug("Hello World", ULID.nextULID())
        assertEquals("hello-world", slug)
    }

    @Test
    fun `generateUniqueSlug should append suffix on conflict`() {
        val existing = mockk<Post>()
        every { existing.id } returns ULID.nextULID() // different id
        every { postRepository.findBySlug("hello-world") } returns existing
        every { postRepository.findBySlug("hello-world-2") } returns null

        val slug = postService.generateUniqueSlug("Hello World", ULID.nextULID())
        assertEquals("hello-world-2", slug)
    }

    @Test
    fun `generateUniqueSlug should skip conflict when it is the same post`() {
        val sameId = ULID.nextULID()
        val existing = mockk<Post>()
        every { existing.id } returns sameId
        every { postRepository.findBySlug("hello-world") } returns existing

        val slug = postService.generateUniqueSlug("Hello World", sameId)
        assertEquals("hello-world", slug) // no suffix — same post
    }

    // -------------------------------------------------------------------------
    // searchPostSummaries
    // -------------------------------------------------------------------------

    @Test
    fun `searchPostSummaries should return page of summaries`() {
        val pageable = mockk<Pageable>()
        val summary = mockk<PostSummary>()
        val page = PageImpl(listOf(summary))
        every { postRepository.searchSummary(pageable, any(), any(), any()) } returns page

        val result =
                postService.searchPostSummaries(pageable, "Category", "Tag", PostStatus.PUBLISHED)
        assertEquals(1, result.content.size)
        assertEquals(summary, result.content[0])
    }
}
