package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.application.audit.AuditLogger
import br.dev.demoraes.abrolhos.domain.entities.Category
import br.dev.demoraes.abrolhos.domain.entities.CategoryName
import br.dev.demoraes.abrolhos.domain.entities.CategorySlug
import br.dev.demoraes.abrolhos.domain.entities.Post
import br.dev.demoraes.abrolhos.domain.entities.PostContent
import br.dev.demoraes.abrolhos.domain.entities.PostSlug
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.entities.PostTitle
import br.dev.demoraes.abrolhos.domain.entities.Role
import br.dev.demoraes.abrolhos.domain.entities.TotpSecret
import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.repository.PostRepository
import br.dev.demoraes.abrolhos.infrastructure.monitoring.MetricsService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import ulid.ULID
import java.time.OffsetDateTime

class ScheduledPublishingServiceTest {

    private val postRepository = mockk<PostRepository>()
    private val metricsService = mockk<MetricsService>(relaxed = true)
    private val auditLogger = mockk<AuditLogger>(relaxed = true)

    private val service = ScheduledPublishingService(postRepository, metricsService, auditLogger)

    private fun makeScheduledPost(
        publishedAt: OffsetDateTime = OffsetDateTime.now().minusMinutes(5)
    ) =
        Post(
            id = ULID.nextULID(),
            author =
            User(
                id = ULID.nextULID(),
                username = Username("author"),
                totpSecret = TotpSecret("JBSWY3DPEHPK3PXP"),
                passwordHash = null,
                isActive = true,
                role = Role.USER,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            ),
            title = PostTitle("Scheduled Post"),
            slug = PostSlug("scheduled-post"),
            content = PostContent("Content"),
            status = PostStatus.SCHEDULED,
            publishedAt = publishedAt,
            category =
            Category(
                id = ULID.nextULID(),
                name = CategoryName("Cat"),
                slug = CategorySlug("cat"),
                posts = emptySet(),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            ),
            tags = emptySet(),
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now(),
        )

    @Test
    fun `publishScheduledPosts should publish posts ready to publish`() {
        val post = makeScheduledPost()
        every { postRepository.findScheduledPostsReadyToPublish(any()) } returns listOf(post)
        every { postRepository.save(any()) } answers { firstArg() }

        service.publishScheduledPosts()

        verify { postRepository.save(match { it.status == PostStatus.PUBLISHED }) }
        verify { metricsService.recordPostAutoPublished() }
        verify { auditLogger.logPostAutoPublished(any(), any(), any()) }
        verify { metricsService.recordScheduledPublishingJobTime(any()) }
    }

    @Test
    fun `publishScheduledPosts should do nothing when no posts are ready`() {
        every { postRepository.findScheduledPostsReadyToPublish(any()) } returns emptyList()

        service.publishScheduledPosts()

        verify(exactly = 0) { postRepository.save(any()) }
        verify(exactly = 0) { metricsService.recordPostAutoPublished() }
        // Job timer still recorded
        verify { metricsService.recordScheduledPublishingJobTime(any()) }
    }

    @Test
    fun `publishScheduledPosts should continue processing when one post fails`() {
        val failingPost = makeScheduledPost()
        val goodPost =
            makeScheduledPost()
                .copy(slug = PostSlug("good-post"), title = PostTitle("Good Post"))
        every { postRepository.findScheduledPostsReadyToPublish(any()) } returns
            listOf(failingPost, goodPost)
        every { postRepository.save(match { it.slug.value == "scheduled-post" }) } throws
            RuntimeException("DB error")
        every { postRepository.save(match { it.slug.value == "good-post" }) } answers { firstArg() }

        // Should not throw — error is caught per-post
        service.publishScheduledPosts()

        // Good post should still be saved
        verify(exactly = 1) { postRepository.save(match { it.slug.value == "good-post" }) }
        // Timer still recorded
        verify { metricsService.recordScheduledPublishingJobTime(any()) }
    }

    @Test
    fun `publishScheduledPosts should preserve scheduledAt as publishedAt when already set`() {
        val scheduledAt = OffsetDateTime.now().minusHours(1)
        val post = makeScheduledPost(publishedAt = scheduledAt)
        every { postRepository.findScheduledPostsReadyToPublish(any()) } returns listOf(post)
        every { postRepository.save(any()) } answers { firstArg() }

        service.publishScheduledPosts()

        verify { postRepository.save(match { it.publishedAt == scheduledAt }) }
    }
}
