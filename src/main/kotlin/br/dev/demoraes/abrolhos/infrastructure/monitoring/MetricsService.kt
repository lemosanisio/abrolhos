package br.dev.demoraes.abrolhos.infrastructure.monitoring

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import java.time.Duration
import org.springframework.stereotype.Service

/**
 * Service for recording custom business metrics.
 *
 * Exposes counters and timers for authentication and post operations, making them available via the
 * `/actuator/prometheus` endpoint for VictoriaMetrics scraping.
 */
@Service
class MetricsService(private val meterRegistry: MeterRegistry) {

    // Authentication metrics
    private val loginAttempts: Counter =
            Counter.builder("auth.login.attempts")
                    .description("Total number of login attempts")
                    .register(meterRegistry)

    private val loginSuccesses: Counter =
            Counter.builder("auth.login.success")
                    .description("Total number of successful logins")
                    .register(meterRegistry)

    private val loginFailuresBuilder =
            Counter.builder("auth.login.failure")
                    .description("Total number of failed logins")

    // Post metrics
    private val postCreations: Counter =
            Counter.builder("posts.created")
                    .description("Total number of posts created")
                    .register(meterRegistry)

    private val postViews: Counter =
            Counter.builder("posts.views")
                    .description("Total number of post views")
                    .register(meterRegistry)

    private val postUpdates: Counter =
            Counter.builder("posts.updated")
                    .description("Total number of posts updated")
                    .register(meterRegistry)

    private val postDeletions: Counter =
            Counter.builder("posts.deleted")
                    .description("Total number of posts deleted")
                    .register(meterRegistry)

    private val postAutoPublished: Counter =
            Counter.builder("posts.auto_published")
                    .description("Total number of posts auto-published by the scheduled job")
                    .register(meterRegistry)

    // Timers
    private val postQueryTimer: Timer =
            Timer.builder("posts.query.time")
                    .description("Time taken to query posts")
                    .register(meterRegistry)

    private val scheduledPublishingJobTimer: Timer =
            Timer.builder("posts.scheduled_publishing.time")
                    .description("Time taken to run the scheduled publishing job")
                    .register(meterRegistry)

    // Cache metrics
    private val cacheHits: Counter =
            Counter.builder("cache.hits")
                    .description("Total number of cache hits")
                    .register(meterRegistry)

    private val cacheMisses: Counter =
            Counter.builder("cache.misses")
                    .description("Total number of cache misses")
                    .register(meterRegistry)

    /** Records a single login attempt (independent of outcome). */
    fun recordLoginAttempt() = loginAttempts.increment()

    /** Records a successful authentication flow leading to token generation. */
    fun recordLoginSuccess() = loginSuccesses.increment()

    /** Records any failure during the authentication flow (bad credentials, inactive, etc.). */
    fun recordLoginFailure(outcome: String) = 
        loginFailuresBuilder.tag("outcome", outcome).register(meterRegistry).increment()

    /** Records a successful post creation via POST /api/posts. */
    fun recordPostCreation() = postCreations.increment()

    /** Records a single read request to a published post. */
    fun recordPostView() = postViews.increment()

    /** Records a successful post update via PUT /api/posts/{slug}. */
    fun recordPostUpdate() = postUpdates.increment()

    /** Records a successful post soft-delete via DELETE /api/posts/{slug}. */
    fun recordPostDeletion() = postDeletions.increment()

    /** Records a post auto-published by the scheduled publishing job. */
    fun recordPostAutoPublished() = postAutoPublished.increment()

    /**
     * Records the database query execution time for listing/searching posts.
     * @param duration The exact duration measured via [System.nanoTime].
     */
    fun recordPostQueryTime(duration: Duration) {
        postQueryTimer.record(duration)
    }

    /**
     * Records the total execution time of a scheduled publishing job run.
     * @param duration The exact duration measured via [System.nanoTime].
     */
    fun recordScheduledPublishingJobTime(duration: Duration) {
        scheduledPublishingJobTimer.record(duration)
    }

    /** Records a single cache hit. */
    fun recordCacheHit() = cacheHits.increment()

    /** Records a single cache miss. */
    fun recordCacheMiss() = cacheMisses.increment()
}
