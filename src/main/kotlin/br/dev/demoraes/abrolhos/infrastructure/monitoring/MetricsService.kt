package br.dev.demoraes.abrolhos.infrastructure.monitoring

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import java.time.Duration

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

    private val loginFailures: Counter =
        Counter.builder("auth.login.failure")
            .description("Total number of failed logins")
            .register(meterRegistry)

    // Post metrics
    private val postCreations: Counter =
        Counter.builder("posts.created")
            .description("Total number of posts created")
            .register(meterRegistry)

    private val postViews: Counter =
        Counter.builder("posts.views")
            .description("Total number of post views")
            .register(meterRegistry)

    // Timers
    private val postQueryTimer: Timer =
        Timer.builder("posts.query.time")
            .description("Time taken to query posts")
            .register(meterRegistry)

    /** Records a single login attempt (independent of outcome). */
    fun recordLoginAttempt() = loginAttempts.increment()

    /** Records a successful authentication flow leading to token generation. */
    fun recordLoginSuccess() = loginSuccesses.increment()

    /** Records any failure during the authentication flow (bad credentials, inactive, etc.). */
    fun recordLoginFailure() = loginFailures.increment()

    /** Records a successful post creation via POST /api/posts. */
    fun recordPostCreation() = postCreations.increment()

    /** Records a single read request to a post via GET /api/posts/{slug}. */
    fun recordPostView() = postViews.increment()

    /**
     * Records the database query execution time for listing/searching posts.
     * @param duration The exact duration measured via [System.nanoTime].
     */
    fun recordPostQueryTime(duration: Duration) {
        postQueryTimer.record(duration)
    }
}
