package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.application.audit.AuditLogger
import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.repository.PostRepository
import br.dev.demoraes.abrolhos.infrastructure.monitoring.MetricsService
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.OffsetDateTime

/**
 * Service responsible for automatically publishing scheduled posts.
 *
 * Runs as a background job at a configurable interval (default: 60 seconds). For each post with
 * status SCHEDULED and publishedAt <= now, it transitions the post to PUBLISHED.
 *
 * Errors per individual post are caught and logged so that processing continues for the rest of the
 * batch (Requirement 4.9).
 */
@Service
class ScheduledPublishingService(
    private val postRepository: PostRepository,
    private val metricsService: MetricsService,
    private val auditLogger: AuditLogger,
) {
    private val logger = LoggerFactory.getLogger(ScheduledPublishingService::class.java)

    /**
     * Publishes all scheduled posts whose publishedAt timestamp is on or before the current time.
     *
     * The @Scheduled method is called by the Spring task scheduler *through the bean proxy*, so
     * @CacheEvict is intercepted correctly (no self-invocation issue).
     */
    @Scheduled(fixedDelayString = "\${app.scheduled-publishing.interval:60000}")
    @CacheEvict(value = ["postBySlug", "postSummaries"], allEntries = true)
    @Transactional
    fun publishScheduledPosts() {
        logger.debug("Running scheduled post publishing job")
        val start = System.nanoTime()

        try {
            val now = OffsetDateTime.now()
            val postsToPublish = postRepository.findScheduledPostsReadyToPublish(now)

            if (postsToPublish.isEmpty()) {
                logger.debug("No scheduled posts ready to publish")
            } else {
                logger.info("Found {} post(s) ready to publish", postsToPublish.size)
            }

            postsToPublish.forEach { post ->
                try {
                    val published =
                        post.withUpdatedFields(
                            status = PostStatus.PUBLISHED,
                            publishedAt = post.publishedAt ?: now,
                        )
                    postRepository.save(published)

                    metricsService.recordPostAutoPublished()
                    auditLogger.logPostAutoPublished(
                        postId = post.id.toString(),
                        slug = post.slug.value,
                        scheduledTime = (post.publishedAt ?: now).toString(),
                    )

                    logger.info("Auto-published post: {}", post.slug.value)
                } catch (e: Exception) {
                    logger.error(
                        "Failed to auto-publish post '{}': {}",
                        post.slug.value,
                        e.message,
                        e,
                    )
                }
            }
        } catch (e: Exception) {
            logger.error(
                "Scheduled publishing job encountered an unexpected error: {}",
                e.message,
                e
            )
        } finally {
            metricsService.recordScheduledPublishingJobTime(
                Duration.ofNanos(System.nanoTime() - start)
            )
        }
    }
}
