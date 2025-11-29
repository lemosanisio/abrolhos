package br.dev.demoraes.abrolhos.infrastructure.scheduling

import br.dev.demoraes.abrolhos.domain.entities.PostStatus
import br.dev.demoraes.abrolhos.domain.repository.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

@Component
class ScheduledPublisher(
    private val postRepository: PostRepository,
) {
    private val logger = LoggerFactory.getLogger(ScheduledPublisher::class.java)

    // Runs every minute; adjusts as needed
    @Scheduled(fixedDelay = 60_000)
    fun publishDueScheduledPosts() {
        val now = OffsetDateTime.now()
        val posts = postRepository.findAll()
        var publishedCount = 0
        posts.filterNotNull()
            .filter { it.status == PostStatus.SCHEDULED && it.publishedAt != null && !it.publishedAt.isAfter(now) }
            .forEach { scheduled ->
                val toPublish = scheduled.copy(status = PostStatus.PUBLISHED)
                postRepository.save(toPublish)
                publishedCount++
            }
        if (publishedCount > 0) {
            logger.info("ScheduledPublisher: published {} post(s) at {}", publishedCount, now)
        }
    }
}
