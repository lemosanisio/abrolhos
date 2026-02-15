package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.application.config.SecurityProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import java.util.concurrent.TimeUnit

/**
 * Unit tests for RateLimitService.
 * Tests requirements: 2.1, 2.2, 2.4, 2.8, 2.9, 2.12
 */
class RateLimitServiceTest {

    private lateinit var redisTemplate: RedisTemplate<String, String>
    private lateinit var zSetOperations: ZSetOperations<String, String>
    private lateinit var securityProperties: SecurityProperties
    private lateinit var rateLimitService: RateLimitService

    @BeforeEach
    fun setup() {
        redisTemplate = mockk(relaxed = true)
        zSetOperations = mockk(relaxed = true)
        securityProperties = createSecurityProperties(maxRequests = 5, windowMinutes = 15)

        every { redisTemplate.opsForZSet() } returns zSetOperations

        rateLimitService = RateLimitService(redisTemplate, securityProperties)
    }

    // Requirement 2.1: Track request count per client identifier
    @Test
    fun `should track request for client within limit`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        // Mock: No previous requests
        every { zSetOperations.count(key, any(), any()) } returns 0

        val result = rateLimitService.tryConsume(clientId, endpoint)

        assertTrue(result.isAllowed)
        assertEquals(5, result.limit)
        assertEquals(4, result.remaining)
        assertEquals(0, result.retryAfterSeconds)

        // Verify request was tracked
        verify { zSetOperations.add(key, any(), any()) }
        verify { redisTemplate.expire(key, 15L, TimeUnit.MINUTES) }
    }

    @Test
    fun `should track multiple requests for same client`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        // First request: 0 previous requests
        every { zSetOperations.count(key, any(), any()) } returns 0
        val result1 = rateLimitService.tryConsume(clientId, endpoint)
        assertTrue(result1.isAllowed)
        assertEquals(4, result1.remaining)

        // Second request: 1 previous request
        every { zSetOperations.count(key, any(), any()) } returns 1
        val result2 = rateLimitService.tryConsume(clientId, endpoint)
        assertTrue(result2.isAllowed)
        assertEquals(3, result2.remaining)

        // Third request: 2 previous requests
        every { zSetOperations.count(key, any(), any()) } returns 2
        val result3 = rateLimitService.tryConsume(clientId, endpoint)
        assertTrue(result3.isAllowed)
        assertEquals(2, result3.remaining)
    }

    // Requirement 2.2: Reject requests exceeding maximum attempts
    @Test
    fun `should reject request when limit exceeded`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        // Mock: Already at limit (5 requests)
        every { zSetOperations.count(key, any(), any()) } returns 5

        val result = rateLimitService.tryConsume(clientId, endpoint)

        assertFalse(result.isAllowed)
        assertEquals(5, result.limit)
        assertEquals(0, result.remaining)
        assertTrue(result.retryAfterSeconds > 0)

        // Verify request was NOT tracked (limit exceeded)
        verify(exactly = 0) { zSetOperations.add(key, any(), any()) }
    }

    @Test
    fun `should allow exactly maxRequests requests`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        // Test requests 1-5 (should all be allowed)
        for (i in 0 until 5) {
            every { zSetOperations.count(key, any(), any()) } returns i.toLong()
            val result = rateLimitService.tryConsume(clientId, endpoint)
            assertTrue(result.isAllowed, "Request ${i + 1} should be allowed")
            assertEquals(4 - i, result.remaining)
        }

        // Request 6 (should be rejected)
        every { zSetOperations.count(key, any(), any()) } returns 5
        val result = rateLimitService.tryConsume(clientId, endpoint)
        assertFalse(result.isAllowed, "Request 6 should be rejected")
    }

    // Requirement 2.4: Reset count when time window expires
    @Test
    fun `should remove old entries outside sliding window`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        every { zSetOperations.count(key, any(), any()) } returns 0

        rateLimitService.tryConsume(clientId, endpoint)

        // Verify old entries are removed
        verify { zSetOperations.removeRangeByScore(key, 0.0, any()) }
    }

    @Test
    fun `should set expiration on rate limit key`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        every { zSetOperations.count(key, any(), any()) } returns 0

        rateLimitService.tryConsume(clientId, endpoint)

        // Verify key expiration is set to window duration
        verify { redisTemplate.expire(key, 15L, TimeUnit.MINUTES) }
    }

    // Requirement 2.8: Use Redis for distributed rate limiting
    @Test
    fun `should use Redis ZSET for tracking requests`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        every { zSetOperations.count(key, any(), any()) } returns 0

        rateLimitService.tryConsume(clientId, endpoint)

        // Verify Redis ZSET operations are used
        verify { redisTemplate.opsForZSet() }
        verify { zSetOperations.removeRangeByScore(key, any(), any()) }
        verify { zSetOperations.count(key, any(), any()) }
        verify { zSetOperations.add(key, any(), any()) }
    }

    @Test
    fun `should use correct Redis key format`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val expectedKey = "rate_limit:/api/auth/login:192.168.1.100"

        every { zSetOperations.count(any(), any(), any()) } returns 0

        rateLimitService.tryConsume(clientId, endpoint)

        verify { zSetOperations.removeRangeByScore(expectedKey, any(), any()) }
        verify { zSetOperations.count(expectedKey, any(), any()) }
        verify { zSetOperations.add(expectedKey, any(), any()) }
    }

    // Requirement 2.9: Apply exponential backoff
    @Test
    fun `should apply exponential backoff for first violation`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        // First violation (6th request)
        every { zSetOperations.count(key, any(), any()) } returns 5

        val result = rateLimitService.tryConsume(clientId, endpoint)

        assertFalse(result.isAllowed)
        // Backoff multiplier should be 1 (2^0 = 1)
        // Reset time should be approximately: now + 15 minutes + (15 minutes * 1)
        val expectedMinRetry = (15 * 60) // 15 minutes in seconds
        val expectedMaxRetry = (30 * 60) + 10 // 30 minutes + buffer
        assertTrue(result.retryAfterSeconds >= expectedMinRetry)
        assertTrue(result.retryAfterSeconds <= expectedMaxRetry)
    }

    @Test
    fun `should apply exponential backoff for second violation`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        // Second violation (7th request)
        every { zSetOperations.count(key, any(), any()) } returns 6

        val result = rateLimitService.tryConsume(clientId, endpoint)

        assertFalse(result.isAllowed)
        // Backoff multiplier should be 2 (2^1 = 2)
        // Reset time should be approximately: now + 15 minutes + (15 minutes * 2)
        val expectedMinRetry = (30 * 60) // 30 minutes in seconds
        val expectedMaxRetry = (45 * 60) + 10 // 45 minutes + buffer
        assertTrue(result.retryAfterSeconds >= expectedMinRetry)
        assertTrue(result.retryAfterSeconds <= expectedMaxRetry)
    }

    @Test
    fun `should apply exponential backoff for third violation`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        // Third violation (8th request)
        every { zSetOperations.count(key, any(), any()) } returns 7

        val result = rateLimitService.tryConsume(clientId, endpoint)

        assertFalse(result.isAllowed)
        // Backoff multiplier should be 4 (2^2 = 4)
        // Reset time should be approximately: now + 15 minutes + (15 minutes * 4)
        val expectedMinRetry = (60 * 60) // 60 minutes in seconds
        val expectedMaxRetry = (75 * 60) + 10 // 75 minutes + buffer
        assertTrue(result.retryAfterSeconds >= expectedMinRetry)
        assertTrue(result.retryAfterSeconds <= expectedMaxRetry)
    }

    @Test
    fun `should cap exponential backoff at 8x multiplier`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        // Many violations (should cap at 8x)
        every { zSetOperations.count(key, any(), any()) } returns 20

        val result = rateLimitService.tryConsume(clientId, endpoint)

        assertFalse(result.isAllowed)
        // Backoff multiplier should be capped at 8
        // Reset time should be approximately: now + 15 minutes + (15 minutes * 8)
        val expectedMinRetry = (120 * 60) // 120 minutes in seconds
        val expectedMaxRetry = (135 * 60) + 10 // 135 minutes + buffer
        assertTrue(result.retryAfterSeconds >= expectedMinRetry)
        assertTrue(result.retryAfterSeconds <= expectedMaxRetry)
    }

    // Requirement 2.12: Graceful degradation for Redis failures
    @Test
    fun `should fail open when Redis is unavailable`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"

        // Mock Redis failure
        every { redisTemplate.opsForZSet() } throws RuntimeException("Redis connection failed")

        val result = rateLimitService.tryConsume(clientId, endpoint)

        // Should allow request when Redis fails (fail open)
        assertTrue(result.isAllowed)
        assertEquals(5, result.limit)
        assertEquals(5, result.remaining)
        assertEquals(0, result.retryAfterSeconds)
    }

    @Test
    fun `should fail open when Redis count operation fails`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"

        // Mock Redis operation failure
        every { zSetOperations.count(any(), any(), any()) } throws RuntimeException("Redis operation failed")

        val result = rateLimitService.tryConsume(clientId, endpoint)

        // Should allow request when Redis fails (fail open)
        assertTrue(result.isAllowed)
    }

    @Test
    fun `should fail open when Redis add operation fails`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"

        every { zSetOperations.count(any(), any(), any()) } returns 0
        every { zSetOperations.add(any(), any(), any()) } throws RuntimeException("Redis add failed")

        val result = rateLimitService.tryConsume(clientId, endpoint)

        // Should allow request when Redis fails (fail open)
        assertTrue(result.isAllowed)
    }

    // Edge cases
    @Test
    fun `should handle different endpoints independently`() {
        val clientId = "192.168.1.100"
        val endpoint1 = "/api/auth/login"
        val endpoint2 = "/api/auth/activate"
        val key1 = "rate_limit:$endpoint1:$clientId"
        val key2 = "rate_limit:$endpoint2:$clientId"

        every { zSetOperations.count(key1, any(), any()) } returns 0
        every { zSetOperations.count(key2, any(), any()) } returns 0

        val result1 = rateLimitService.tryConsume(clientId, endpoint1)
        val result2 = rateLimitService.tryConsume(clientId, endpoint2)

        assertTrue(result1.isAllowed)
        assertTrue(result2.isAllowed)

        // Verify separate keys are used
        verify { zSetOperations.add(key1, any(), any()) }
        verify { zSetOperations.add(key2, any(), any()) }
    }

    @Test
    fun `should handle different clients independently`() {
        val clientId1 = "192.168.1.100"
        val clientId2 = "192.168.1.101"
        val endpoint = "/api/auth/login"
        val key1 = "rate_limit:$endpoint:$clientId1"
        val key2 = "rate_limit:$endpoint:$clientId2"

        every { zSetOperations.count(key1, any(), any()) } returns 0
        every { zSetOperations.count(key2, any(), any()) } returns 0

        val result1 = rateLimitService.tryConsume(clientId1, endpoint)
        val result2 = rateLimitService.tryConsume(clientId2, endpoint)

        assertTrue(result1.isAllowed)
        assertTrue(result2.isAllowed)

        // Verify separate keys are used
        verify { zSetOperations.add(key1, any(), any()) }
        verify { zSetOperations.add(key2, any(), any()) }
    }

    @Test
    fun `should handle custom rate limit configuration`() {
        val customProperties = createSecurityProperties(maxRequests = 10, windowMinutes = 30)
        val customService = RateLimitService(redisTemplate, customProperties)

        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        every { zSetOperations.count(key, any(), any()) } returns 0

        val result = customService.tryConsume(clientId, endpoint)

        assertTrue(result.isAllowed)
        assertEquals(10, result.limit)
        assertEquals(9, result.remaining)

        // Verify custom window duration is used
        verify { redisTemplate.expire(key, 30L, TimeUnit.MINUTES) }
    }

    @Test
    fun `should handle IPv6 addresses as client identifiers`() {
        val clientId = "2001:0db8:85a3:0000:0000:8a2e:0370:7334"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        every { zSetOperations.count(key, any(), any()) } returns 0

        val result = rateLimitService.tryConsume(clientId, endpoint)

        assertTrue(result.isAllowed)
        verify { zSetOperations.add(key, any(), any()) }
    }

    @Test
    fun `should handle special characters in endpoint paths`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/invite/abc-123_xyz"
        val key = "rate_limit:$endpoint:$clientId"

        every { zSetOperations.count(key, any(), any()) } returns 0

        val result = rateLimitService.tryConsume(clientId, endpoint)

        assertTrue(result.isAllowed)
        verify { zSetOperations.add(key, any(), any()) }
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

    // Additional edge cases for Task 6.4
    @Test
    fun `should handle empty client identifier`() {
        val clientId = ""
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        every { zSetOperations.count(key, any(), any()) } returns 0

        val result = rateLimitService.tryConsume(clientId, endpoint)

        assertTrue(result.isAllowed)
        verify { zSetOperations.add(key, any(), any()) }
    }

    @Test
    fun `should handle empty endpoint path`() {
        val clientId = "192.168.1.100"
        val endpoint = ""
        val key = "rate_limit:$endpoint:$clientId"

        every { zSetOperations.count(key, any(), any()) } returns 0

        val result = rateLimitService.tryConsume(clientId, endpoint)

        assertTrue(result.isAllowed)
        verify { zSetOperations.add(key, any(), any()) }
    }

    @Test
    fun `should handle very long client identifier`() {
        val clientId = "x".repeat(1000)
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        every { zSetOperations.count(key, any(), any()) } returns 0

        val result = rateLimitService.tryConsume(clientId, endpoint)

        assertTrue(result.isAllowed)
        verify { zSetOperations.add(key, any(), any()) }
    }

    @Test
    fun `should handle very long endpoint path`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/" + "x".repeat(1000)
        val key = "rate_limit:$endpoint:$clientId"

        every { zSetOperations.count(key, any(), any()) } returns 0

        val result = rateLimitService.tryConsume(clientId, endpoint)

        assertTrue(result.isAllowed)
        verify { zSetOperations.add(key, any(), any()) }
    }

    @Test
    fun `should handle minimum rate limit configuration (1 request)`() {
        val customProperties = createSecurityProperties(maxRequests = 1, windowMinutes = 1)
        val customService = RateLimitService(redisTemplate, customProperties)

        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        // First request should be allowed
        every { zSetOperations.count(key, any(), any()) } returns 0
        val result1 = customService.tryConsume(clientId, endpoint)
        assertTrue(result1.isAllowed)
        assertEquals(0, result1.remaining)

        // Second request should be rejected
        every { zSetOperations.count(key, any(), any()) } returns 1
        val result2 = customService.tryConsume(clientId, endpoint)
        assertFalse(result2.isAllowed)
    }

    @Test
    fun `should handle very large rate limit configuration`() {
        val customProperties = createSecurityProperties(maxRequests = 10000, windowMinutes = 1440)
        val customService = RateLimitService(redisTemplate, customProperties)

        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        every { zSetOperations.count(key, any(), any()) } returns 0

        val result = customService.tryConsume(clientId, endpoint)

        assertTrue(result.isAllowed)
        assertEquals(10000, result.limit)
        assertEquals(9999, result.remaining)
    }

    @Test
    fun `should handle Redis returning null count`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        // Redis count returns null (treated as 0)
        every { zSetOperations.count(key, any(), any()) } returns null

        val result = rateLimitService.tryConsume(clientId, endpoint)

        // Should treat null as 0 and allow the request
        assertTrue(result.isAllowed)
        assertEquals(5, result.limit)
        assertEquals(4, result.remaining)
    }

    @Test
    fun `should handle concurrent requests at boundary`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        // Simulate exactly at the limit (5th request)
        every { zSetOperations.count(key, any(), any()) } returns 4

        val result = rateLimitService.tryConsume(clientId, endpoint)

        // 5th request should be allowed (0-indexed: 0,1,2,3,4 = 5 requests)
        assertTrue(result.isAllowed)
        assertEquals(0, result.remaining)
    }

    @Test
    fun `should handle window boundary with zero remaining time`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        every { zSetOperations.count(key, any(), any()) } returns 0

        val result = rateLimitService.tryConsume(clientId, endpoint)

        // Reset time should be in the future
        assertTrue(result.resetTime > System.currentTimeMillis())
        
        // For allowed requests, retry-after should be 0
        assertEquals(0, result.retryAfterSeconds)
    }

    @Test
    fun `should handle Redis removeRangeByScore failure gracefully`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"

        // Mock Redis removeRangeByScore failure
        every { zSetOperations.removeRangeByScore(any(), any(), any()) } throws RuntimeException("Redis removeRangeByScore failed")

        val result = rateLimitService.tryConsume(clientId, endpoint)

        // Should fail open when Redis fails
        assertTrue(result.isAllowed)
    }

    @Test
    fun `should handle Redis expire operation failure gracefully`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        every { zSetOperations.count(key, any(), any()) } returns 0
        every { redisTemplate.expire(any(), any(), any()) } throws RuntimeException("Redis expire failed")

        val result = rateLimitService.tryConsume(clientId, endpoint)

        // Should fail open when Redis fails
        assertTrue(result.isAllowed)
    }

    @Test
    fun `should calculate correct retry-after for first violation with minimum window`() {
        val customProperties = createSecurityProperties(maxRequests = 5, windowMinutes = 1)
        val customService = RateLimitService(redisTemplate, customProperties)

        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        // First violation (6th request)
        every { zSetOperations.count(key, any(), any()) } returns 5

        val result = customService.tryConsume(clientId, endpoint)

        assertFalse(result.isAllowed)
        // With 1 minute window and 1x backoff, retry-after should be approximately 2 minutes (120 seconds)
        assertTrue(result.retryAfterSeconds >= 60) // At least 1 minute
        assertTrue(result.retryAfterSeconds <= 130) // At most ~2 minutes + buffer
    }

    @Test
    fun `should handle client identifier with special Redis characters`() {
        val clientId = "client:with:colons:192.168.1.100"
        val endpoint = "/api/auth/login"
        val key = "rate_limit:$endpoint:$clientId"

        every { zSetOperations.count(key, any(), any()) } returns 0

        val result = rateLimitService.tryConsume(clientId, endpoint)

        assertTrue(result.isAllowed)
        verify { zSetOperations.add(key, any(), any()) }
    }

    @Test
    fun `should handle endpoint with query parameters`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/login?redirect=/dashboard"
        val key = "rate_limit:$endpoint:$clientId"

        every { zSetOperations.count(key, any(), any()) } returns 0

        val result = rateLimitService.tryConsume(clientId, endpoint)

        assertTrue(result.isAllowed)
        verify { zSetOperations.add(key, any(), any()) }
    }

    @Test
    fun `should handle endpoint with URL encoding`() {
        val clientId = "192.168.1.100"
        val endpoint = "/api/auth/invite/token%20with%20spaces"
        val key = "rate_limit:$endpoint:$clientId"

        every { zSetOperations.count(key, any(), any()) } returns 0

        val result = rateLimitService.tryConsume(clientId, endpoint)

        assertTrue(result.isAllowed)
        verify { zSetOperations.add(key, any(), any()) }
    }
}
