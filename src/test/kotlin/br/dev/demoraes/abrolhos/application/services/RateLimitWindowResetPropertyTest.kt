package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.application.config.SecurityProperties
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations

/**
 * Property-based test for rate limit window reset behavior.
 *
 * **Property 5: Rate limit window reset**
 * **Validates: Requirements 2.4**
 *
 * This test verifies that the rate limiter correctly resets the request count
 * after the time window expires:
 * For any client that exceeds the rate limit, after the time window expires,
 * the client should be able to make requests again.
 *
 * Property: ∀ client. (exceeded_limit ∧ window_expired) → can_make_requests
 */
class RateLimitWindowResetPropertyTest {

    private lateinit var redisTemplate: RedisTemplate<String, String>
    private lateinit var zSetOperations: ZSetOperations<String, String>
    private lateinit var rateLimitService: RateLimitService

    @BeforeEach
    fun setup() {
        redisTemplate = mockk(relaxed = true)
        zSetOperations = mockk(relaxed = true)
        every { redisTemplate.opsForZSet() } returns zSetOperations
    }

    /**
     * Property: For any client, old requests outside the sliding window should be removed.
     *
     * This property verifies that the rate limiter correctly removes old entries
     * that fall outside the current sliding window, effectively resetting the count.
     */
    @Test
    fun `property - old requests outside window are removed`() {
        repeat(100) {
            // Generate arbitrary rate limit configuration
            val maxRequests = (3..15).random()
            val windowMinutes = (5..30).random()
            val clientId = "192.168.1.${(1..255).random()}"
            val endpoint = "/api/auth/login"

            // Setup service with random configuration
            val securityProperties = createSecurityProperties(maxRequests, windowMinutes)
            rateLimitService = RateLimitService(redisTemplate, securityProperties)

            val key = "rate_limit:$endpoint:$clientId"
            every { zSetOperations.count(key, any(), any()) } returns 0

            // When: A request is made
            rateLimitService.tryConsume(clientId, endpoint)

            // Then: Old entries should be removed from the sliding window
            // The removeRangeByScore should be called with a timestamp representing
            // the start of the current window (now - windowMinutes)
            verify { zSetOperations.removeRangeByScore(key, 0.0, any()) }
        }
    }

    /**
     * Property: For any client with requests only outside the window, the count should be zero.
     *
     * This property verifies that when all previous requests are outside the sliding window,
     * the effective count is zero and new requests are allowed.
     */
    @Test
    fun `property - requests outside window do not count toward limit`() {
        repeat(100) {
            // Generate arbitrary rate limit configuration
            val maxRequests = (3..15).random()
            val windowMinutes = (5..30).random()
            val clientId = "192.168.1.${(1..255).random()}"
            val endpoint = "/api/auth/login"

            // Setup service with random configuration
            val securityProperties = createSecurityProperties(maxRequests, windowMinutes)
            rateLimitService = RateLimitService(redisTemplate, securityProperties)

            val key = "rate_limit:$endpoint:$clientId"

            // Simulate: All previous requests are outside the window (count = 0)
            every { zSetOperations.count(key, any(), any()) } returns 0

            // When: A new request is made
            val result = rateLimitService.tryConsume(clientId, endpoint)

            // Then: The request should be allowed (window has reset)
            result.isAllowed shouldBe true
            result.remaining shouldBe (maxRequests - 1)
        }
    }

    /**
     * Property: For any client that previously exceeded the limit, if the window resets,
     * they should be able to make maxRequests new requests.
     *
     * This property verifies the complete reset behavior: after a window reset,
     * a client can make the full quota of requests again.
     */
    @Test
    fun `property - client can make full quota after window reset`() {
        repeat(100) {
            // Generate arbitrary rate limit configuration
            val maxRequests = (3..15).random()
            val windowMinutes = (5..30).random()
            val clientId = "192.168.1.${(1..255).random()}"
            val endpoint = "/api/auth/login"

            // Setup service with random configuration
            val securityProperties = createSecurityProperties(maxRequests, windowMinutes)
            rateLimitService = RateLimitService(redisTemplate, securityProperties)

            val key = "rate_limit:$endpoint:$clientId"

            // Scenario: Client previously exceeded limit
            every { zSetOperations.count(key, any(), any()) } returns maxRequests.toLong()
            val exceededResult = rateLimitService.tryConsume(clientId, endpoint)
            exceededResult.isAllowed shouldBe false

            // Scenario: Window resets (all old requests removed, count = 0)
            every { zSetOperations.count(key, any(), any()) } returns 0

            // When: Client makes new requests after window reset
            for (i in 0 until maxRequests) {
                every { zSetOperations.count(key, any(), any()) } returns i.toLong()
                val result = rateLimitService.tryConsume(clientId, endpoint)

                // Then: All maxRequests should be allowed again
                result.isAllowed shouldBe true
                result.remaining shouldBe (maxRequests - i - 1)
            }
        }
    }

    /**
     * Property: For any window configuration, the sliding window calculation should be consistent.
     *
     * This property verifies that the window start time is calculated correctly
     * based on the configured window duration.
     */
    @Test
    fun `property - sliding window calculation is consistent`() {
        repeat(100) {
            // Generate arbitrary rate limit configuration
            val maxRequests = (3..15).random()
            val windowMinutes = (5..60).random()
            val clientId = "192.168.1.${(1..255).random()}"
            val endpoint = "/api/auth/login"

            // Setup service with random configuration
            val securityProperties = createSecurityProperties(maxRequests, windowMinutes)
            rateLimitService = RateLimitService(redisTemplate, securityProperties)

            val key = "rate_limit:$endpoint:$clientId"
            every { zSetOperations.count(key, any(), any()) } returns 0

            val beforeTime = System.currentTimeMillis()
            rateLimitService.tryConsume(clientId, endpoint)
            val afterTime = System.currentTimeMillis()

            // Verify that removeRangeByScore was called with a window start time
            // that is approximately (now - windowMinutes)
            val expectedWindowStart = beforeTime - (windowMinutes * 60 * 1000)
            val expectedWindowEnd = afterTime - (windowMinutes * 60 * 1000)

            verify {
                zSetOperations.removeRangeByScore(
                    key,
                    0.0,
                    match { timestamp ->
                        timestamp >= expectedWindowStart.toDouble() &&
                            timestamp <= expectedWindowEnd.toDouble()
                    }
                )
            }
        }
    }

    /**
     * Property: For any client, the reset time should be consistent with the window duration.
     *
     * This property verifies that the resetTime returned in the result is calculated
     * correctly based on the current time and window duration.
     */
    @Test
    fun `property - reset time is calculated correctly`() {
        repeat(100) {
            // Generate arbitrary rate limit configuration
            val maxRequests = (3..15).random()
            val windowMinutes = (5..60).random()
            val clientId = "192.168.1.${(1..255).random()}"
            val endpoint = "/api/auth/login"

            // Setup service with random configuration
            val securityProperties = createSecurityProperties(maxRequests, windowMinutes)
            rateLimitService = RateLimitService(redisTemplate, securityProperties)

            val key = "rate_limit:$endpoint:$clientId"
            every { zSetOperations.count(key, any(), any()) } returns 0

            val beforeTime = System.currentTimeMillis()
            val result = rateLimitService.tryConsume(clientId, endpoint)
            val afterTime = System.currentTimeMillis()

            // The reset time should be approximately (now + windowMinutes)
            val expectedMinResetTime = beforeTime + (windowMinutes * 60 * 1000)
            val expectedMaxResetTime = afterTime + (windowMinutes * 60 * 1000)

            assert(result.resetTime >= expectedMinResetTime) {
                "Reset time ${result.resetTime} should be >= $expectedMinResetTime"
            }
            assert(result.resetTime <= expectedMaxResetTime) {
                "Reset time ${result.resetTime} should be <= $expectedMaxResetTime"
            }
        }
    }

    /**
     * Property: For any client that exceeds the limit, the retry-after time should reflect
     * when the window will reset (including backoff).
     *
     * This property verifies that the retryAfterSeconds value correctly indicates
     * when the client can retry, accounting for the window reset and exponential backoff.
     */
    @Test
    fun `property - retry-after reflects window reset time with backoff`() {
        repeat(100) {
            // Generate arbitrary rate limit configuration
            val maxRequests = (3..10).random()
            val windowMinutes = (5..30).random()
            val clientId = "192.168.1.${(1..255).random()}"
            val endpoint = "/api/auth/login"

            // Setup service with random configuration
            val securityProperties = createSecurityProperties(maxRequests, windowMinutes)
            rateLimitService = RateLimitService(redisTemplate, securityProperties)

            val key = "rate_limit:$endpoint:$clientId"

            // Simulate exceeding the limit
            every { zSetOperations.count(key, any(), any()) } returns maxRequests.toLong()

            val beforeTime = System.currentTimeMillis()
            val result = rateLimitService.tryConsume(clientId, endpoint)
            val afterTime = System.currentTimeMillis()

            // The retry-after should be at least the window duration
            val minRetrySeconds = (windowMinutes * 60).toLong()
            assert(result.retryAfterSeconds >= minRetrySeconds) {
                "Retry-after ${result.retryAfterSeconds}s should be >= ${minRetrySeconds}s"
            }

            // The retry-after should not be unreasonably large (max 8x backoff)
            val maxRetrySeconds = (windowMinutes * 60 * 9).toLong() // 8x backoff + window + buffer
            assert(result.retryAfterSeconds <= maxRetrySeconds) {
                "Retry-after ${result.retryAfterSeconds}s should be <= ${maxRetrySeconds}s"
            }
        }
    }

    /**
     * Property: For any configuration, partial window overlap should be handled correctly.
     *
     * This property verifies that when some requests are inside the window and some
     * are outside, only the requests inside the window count toward the limit.
     */
    @Test
    fun `property - partial window overlap is handled correctly`() {
        repeat(100) {
            // Generate arbitrary rate limit configuration
            val maxRequests = (5..15).random()
            val windowMinutes = (5..30).random()
            val clientId = "192.168.1.${(1..255).random()}"
            val endpoint = "/api/auth/login"

            // Setup service with random configuration
            val securityProperties = createSecurityProperties(maxRequests, windowMinutes)
            rateLimitService = RateLimitService(redisTemplate, securityProperties)

            val key = "rate_limit:$endpoint:$clientId"

            // Simulate: Some requests inside the window (less than max)
            val requestsInWindow = (1 until maxRequests).random()
            every { zSetOperations.count(key, any(), any()) } returns requestsInWindow.toLong()

            // When: A new request is made
            val result = rateLimitService.tryConsume(clientId, endpoint)

            // Then: The request should be allowed
            result.isAllowed shouldBe true
            result.remaining shouldBe (maxRequests - requestsInWindow - 1)
        }
    }

    // Helper methods
    private fun createSecurityProperties(maxRequests: Int, windowMinutes: Int): SecurityProperties {
        val properties = SecurityProperties()
        properties.rateLimit.maxRequests = maxRequests
        properties.rateLimit.windowMinutes = windowMinutes
        properties.cors.allowedOrigins = "http://localhost:3000"
        properties.encryption.key = "dGVzdC1lbmNyeXB0aW9uLWtleS0zMi1ieXRlcw=="
        return properties
    }
}
