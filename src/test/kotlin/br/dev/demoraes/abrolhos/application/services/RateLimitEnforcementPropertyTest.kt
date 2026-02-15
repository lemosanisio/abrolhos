package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.application.config.SecurityProperties
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations

/**
 * Property-based test for rate limit enforcement.
 *
 * **Property 4: Rate limit enforcement**
 * **Validates: Requirements 2.2, 2.3**
 *
 * This test verifies that the rate limiter correctly enforces the maximum request limit:
 * For any client making N+1 requests within the time window (where N is the limit),
 * the (N+1)th request should be rejected.
 *
 * Property: ∀ N (maxRequests). requests[0..N-1] are allowed ∧ request[N] is rejected
 */
class RateLimitEnforcementPropertyTest {

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
     * Property: For any rate limit configuration, the (N+1)th request should be rejected.
     *
     * This property verifies that the rate limiter enforces the maximum request limit
     * across different configurations and scenarios.
     */
    @Test
    fun `property - rate limiter rejects request when limit exceeded`() {
        var successfulTests = 0

        while (successfulTests < 100) {
            // Generate arbitrary rate limit configuration
            val maxRequests = (1..20).random()
            val windowMinutes = (1..60).random()
            val clientId = "192.168.1.${(1..255).random()}"
            val endpoint = "/api/auth/${listOf("login", "activate", "invite").random()}"

            // Setup service with random configuration
            val securityProperties = createSecurityProperties(maxRequests, windowMinutes)
            rateLimitService = RateLimitService(redisTemplate, securityProperties)

            val key = "rate_limit:$endpoint:$clientId"

            // Simulate N requests (all should be allowed)
            for (i in 0 until maxRequests) {
                every { zSetOperations.count(key, any(), any()) } returns i.toLong()
                val result = rateLimitService.tryConsume(clientId, endpoint)
                
                // Requirement 2.1, 2.2: First N requests should be allowed
                result.isAllowed shouldBe true
                result.limit shouldBe maxRequests
                result.remaining shouldBe (maxRequests - i - 1)
                result.retryAfterSeconds shouldBe 0
            }

            // Simulate (N+1)th request (should be rejected)
            every { zSetOperations.count(key, any(), any()) } returns maxRequests.toLong()
            val rejectedResult = rateLimitService.tryConsume(clientId, endpoint)

            // Requirement 2.3: (N+1)th request should be rejected
            rejectedResult.isAllowed shouldBe false
            rejectedResult.limit shouldBe maxRequests
            rejectedResult.remaining shouldBe 0
            assert(rejectedResult.retryAfterSeconds > 0) { "retryAfterSeconds should be > 0" }

            successfulTests++
        }
    }

    /**
     * Property: For any client exceeding the limit, subsequent requests should also be rejected.
     *
     * This property verifies that once a client exceeds the rate limit, all subsequent
     * requests continue to be rejected until the window resets.
     */
    @Test
    fun `property - rate limiter continues rejecting requests after limit exceeded`() {
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

            // Simulate multiple requests beyond the limit
            val extraRequests = (1..5).random()
            for (i in 0 until (maxRequests + extraRequests)) {
                every { zSetOperations.count(key, any(), any()) } returns i.toLong()
                val result = rateLimitService.tryConsume(clientId, endpoint)

                if (i < maxRequests) {
                    // First N requests should be allowed
                    result.isAllowed shouldBe true
                } else {
                    // All requests beyond N should be rejected
                    result.isAllowed shouldBe false
                    result.remaining shouldBe 0
                }
            }
        }
    }

    /**
     * Property: For any two different clients, rate limits are enforced independently.
     *
     * This property verifies that rate limiting is applied per client identifier,
     * and one client's requests don't affect another client's rate limit.
     */
    @Test
    fun `property - rate limits are enforced independently per client`() {
        repeat(100) {
            // Generate arbitrary rate limit configuration
            val maxRequests = (3..10).random()
            val windowMinutes = (5..30).random()
            val clientId1 = "192.168.1.${(1..255).random()}"
            val clientId2 = "192.168.2.${(1..255).random()}"
            val endpoint = "/api/auth/login"

            // Setup service with random configuration
            val securityProperties = createSecurityProperties(maxRequests, windowMinutes)
            rateLimitService = RateLimitService(redisTemplate, securityProperties)

            val key1 = "rate_limit:$endpoint:$clientId1"
            val key2 = "rate_limit:$endpoint:$clientId2"

            // Client 1 exceeds the limit
            every { zSetOperations.count(key1, any(), any()) } returns maxRequests.toLong()
            val result1 = rateLimitService.tryConsume(clientId1, endpoint)
            result1.isAllowed shouldBe false

            // Client 2 should still be allowed (independent rate limit)
            every { zSetOperations.count(key2, any(), any()) } returns 0
            val result2 = rateLimitService.tryConsume(clientId2, endpoint)
            result2.isAllowed shouldBe true
        }
    }

    /**
     * Property: For any two different endpoints, rate limits are enforced independently.
     *
     * This property verifies that rate limiting is applied per endpoint,
     * and requests to one endpoint don't affect the rate limit of another endpoint.
     */
    @Test
    fun `property - rate limits are enforced independently per endpoint`() {
        repeat(100) {
            // Generate arbitrary rate limit configuration
            val maxRequests = (3..10).random()
            val windowMinutes = (5..30).random()
            val clientId = "192.168.1.${(1..255).random()}"
            val endpoint1 = "/api/auth/login"
            val endpoint2 = "/api/auth/activate"

            // Setup service with random configuration
            val securityProperties = createSecurityProperties(maxRequests, windowMinutes)
            rateLimitService = RateLimitService(redisTemplate, securityProperties)

            val key1 = "rate_limit:$endpoint1:$clientId"
            val key2 = "rate_limit:$endpoint2:$clientId"

            // Client exceeds limit on endpoint1
            every { zSetOperations.count(key1, any(), any()) } returns maxRequests.toLong()
            val result1 = rateLimitService.tryConsume(clientId, endpoint1)
            result1.isAllowed shouldBe false

            // Client should still be allowed on endpoint2 (independent rate limit)
            every { zSetOperations.count(key2, any(), any()) } returns 0
            val result2 = rateLimitService.tryConsume(clientId, endpoint2)
            result2.isAllowed shouldBe true
        }
    }

    /**
     * Property: For any allowed request, the remaining count should decrease correctly.
     *
     * This property verifies that the remaining request count is accurately tracked
     * and decreases by 1 for each allowed request.
     */
    @Test
    fun `property - remaining count decreases correctly for allowed requests`() {
        repeat(100) {
            // Generate arbitrary rate limit configuration
            val maxRequests = (5..20).random()
            val windowMinutes = (5..30).random()
            val clientId = "192.168.1.${(1..255).random()}"
            val endpoint = "/api/auth/login"

            // Setup service with random configuration
            val securityProperties = createSecurityProperties(maxRequests, windowMinutes)
            rateLimitService = RateLimitService(redisTemplate, securityProperties)

            val key = "rate_limit:$endpoint:$clientId"

            // Track remaining count across requests
            for (i in 0 until maxRequests) {
                every { zSetOperations.count(key, any(), any()) } returns i.toLong()
                val result = rateLimitService.tryConsume(clientId, endpoint)

                result.isAllowed shouldBe true
                result.remaining shouldBe (maxRequests - i - 1)
            }
        }
    }

    /**
     * Property: For any rejected request, the remaining count should be zero.
     *
     * This property verifies that when a request is rejected due to rate limiting,
     * the remaining count is always zero.
     */
    @Test
    fun `property - remaining count is zero for rejected requests`() {
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

            // Simulate request beyond the limit
            val requestCount = maxRequests + (1..10).random()
            every { zSetOperations.count(key, any(), any()) } returns requestCount.toLong()
            val result = rateLimitService.tryConsume(clientId, endpoint)

            result.isAllowed shouldBe false
            result.remaining shouldBe 0
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
