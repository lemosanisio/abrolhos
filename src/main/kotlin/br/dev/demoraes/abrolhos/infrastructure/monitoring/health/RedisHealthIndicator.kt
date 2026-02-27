package br.dev.demoraes.abrolhos.infrastructure.monitoring.health

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * Custom health indicator for the Redis connection.
 *
 * Performs a write/read cycle to verify Redis availability and measures the round-trip latency,
 * reporting it in the health details.
 *
 * This effectively tests both read and write operations instead of just PINGing the server,
 * ensuring that the cache engine is fully operational.
 */
@Component("redisHealth")
class RedisHealthIndicator(private val redisTemplate: StringRedisTemplate) : HealthIndicator {

    /**
     * Executes the read/write check and measures the latency.
     * @return [Health] indicating UP with latency if successful, or DOWN if the cycle fails.
     */
    override fun health(): Health {
        return try {
            val start = System.currentTimeMillis()
            redisTemplate.opsForValue().set("health:check", "ok", Duration.ofSeconds(5))
            val value = redisTemplate.opsForValue().get("health:check")
            val duration = System.currentTimeMillis() - start

            if (value == "ok") {
                Health.up()
                    .withDetail("redis", "available")
                    .withDetail("latency", "${duration}ms")
                    .build()
            } else {
                Health.down()
                    .withDetail("redis", "unexpected response")
                    .withDetail("expected", "ok")
                    .withDetail("actual", value ?: "null")
                    .build()
            }
        } catch (e: Exception) {
            Health.down()
                .withDetail("redis", "unavailable")
                .withDetail("error", e.message ?: "Unknown error")
                .build()
        }
    }
}
