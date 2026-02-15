package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.application.config.SecurityProperties
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * Service for enforcing rate limits on authentication endpoints.
 *
 * Uses Redis ZSET (sorted set) with sliding window algorithm to track requests.
 * Implements exponential backoff for repeated violations and graceful degradation
 * when Redis is unavailable.
 *
 * Requirements: 2.1, 2.2, 2.4, 2.8, 2.9, 2.12
 */
@Service
class RateLimitService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val securityProperties: SecurityProperties
) {

    private val logger = LoggerFactory.getLogger(RateLimitService::class.java)

    /**
     * Result of a rate limit check.
     *
     * @property isAllowed Whether the request is allowed
     * @property limit Maximum requests allowed in the window
     * @property remaining Number of requests remaining in the window
     * @property resetTime Timestamp (milliseconds) when the rate limit resets
     * @property retryAfterSeconds Seconds to wait before retrying (0 if allowed)
     */
    data class RateLimitResult(
        val isAllowed: Boolean,
        val limit: Int,
        val remaining: Int,
        val resetTime: Long,
        val retryAfterSeconds: Long
    )

    /**
     * Attempts to consume a rate limit token for the given client and endpoint.
     *
     * Uses a sliding window algorithm with Redis ZSET:
     * - Score: Request timestamp in milliseconds
     * - Member: Unique request identifier (timestamp as string)
     *
     * Requirement 2.1: Track request count per client identifier
     * Requirement 2.2: Reject requests exceeding maximum attempts
     * Requirement 2.4: Reset count when time window expires
     * Requirement 2.8: Use Redis for distributed rate limiting
     * Requirement 2.9: Apply exponential backoff
     * Requirement 2.12: Graceful degradation for Redis failures
     *
     * @param clientId Client identifier (typically IP address)
     * @param endpoint The endpoint being accessed
     * @return RateLimitResult indicating whether the request is allowed
     */
    fun tryConsume(clientId: String, endpoint: String): RateLimitResult {
        return try {
            val key = buildRateLimitKey(endpoint, clientId)
            val now = System.currentTimeMillis()
            val windowStart = now - (securityProperties.rateLimit.windowMinutes * 60 * 1000)
            val maxRequests = securityProperties.rateLimit.maxRequests

            val operations = redisTemplate.opsForZSet()

            // Requirement 2.4: Remove old entries outside the sliding window
            operations.removeRangeByScore(key, 0.0, windowStart.toDouble())

            // Count requests in current window
            val count = operations.count(key, windowStart.toDouble(), now.toDouble()) ?: 0

            val resetTime = now + (securityProperties.rateLimit.windowMinutes * 60 * 1000)

            if (count < maxRequests) {
                // Requirement 2.1: Track the request
                operations.add(key, now.toString(), now.toDouble())
                redisTemplate.expire(key, securityProperties.rateLimit.windowMinutes.toLong(), TimeUnit.MINUTES)

                RateLimitResult(
                    isAllowed = true,
                    limit = maxRequests,
                    remaining = (maxRequests - count - 1).toInt(),
                    resetTime = resetTime,
                    retryAfterSeconds = 0
                )
            } else {
                // Requirement 2.2: Reject request when limit exceeded
                // Requirement 2.9: Apply exponential backoff
                val backoffMultiplier = calculateBackoffMultiplier(count.toInt(), maxRequests)
                val extendedResetTime = resetTime + (securityProperties.rateLimit.windowMinutes * 60 * 1000 * backoffMultiplier)

                logger.debug(
                    "Rate limit exceeded for client {} on endpoint {}. Count: {}, Limit: {}, Backoff: {}x",
                    clientId, endpoint, count, maxRequests, backoffMultiplier
                )

                RateLimitResult(
                    isAllowed = false,
                    limit = maxRequests,
                    remaining = 0,
                    resetTime = extendedResetTime,
                    retryAfterSeconds = ((extendedResetTime - now) / 1000)
                )
            }
        } catch (e: Exception) {
            // Requirement 2.12: Graceful degradation - fail open if Redis unavailable
            logger.warn("Rate limiting unavailable due to Redis error, allowing request: ${e.message}")
            
            // Return a permissive result when Redis fails
            RateLimitResult(
                isAllowed = true,
                limit = securityProperties.rateLimit.maxRequests,
                remaining = securityProperties.rateLimit.maxRequests,
                resetTime = System.currentTimeMillis() + (securityProperties.rateLimit.windowMinutes * 60 * 1000),
                retryAfterSeconds = 0
            )
        }
    }

    /**
     * Calculates exponential backoff multiplier based on violation count.
     *
     * Requirement 2.9: Exponential backoff calculation
     *
     * Formula: 2^(violations) capped at 8x
     * - First violation: 1x (no additional backoff)
     * - Second violation: 2x
     * - Third violation: 4x
     * - Fourth+ violation: 8x (capped)
     *
     * @param attemptCount Total number of attempts in the window
     * @param maxRequests Maximum allowed requests
     * @return Backoff multiplier (1, 2, 4, or 8)
     */
    private fun calculateBackoffMultiplier(attemptCount: Int, maxRequests: Int): Int {
        val violations = attemptCount - maxRequests
        if (violations <= 0) return 0
        
        // Exponential backoff: 2^violations, capped at 8x
        return minOf(1 shl violations, MAX_BACKOFF_MULTIPLIER)
    }

    /**
     * Builds the Redis key for rate limiting.
     *
     * Format: rate_limit:{endpoint}:{clientId}
     *
     * @param endpoint The endpoint being accessed
     * @param clientId Client identifier
     * @return Redis key string
     */
    private fun buildRateLimitKey(endpoint: String, clientId: String): String {
        return "rate_limit:$endpoint:$clientId"
    }

    companion object {
        private const val MAX_BACKOFF_MULTIPLIER = 8
    }
}
