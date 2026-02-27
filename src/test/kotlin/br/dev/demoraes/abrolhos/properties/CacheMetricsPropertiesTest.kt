package br.dev.demoraes.abrolhos.properties

import br.dev.demoraes.abrolhos.IntegrationTestBase
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.test.context.ActiveProfiles

/**
 * Property-based test for Cache Metrics.
 *
 * **Validates: Requirement 2.3 (Metrics - cache hits/misses)**
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
class CacheMetricsPropertiesTest : IntegrationTestBase() {

    @Autowired private lateinit var cacheManager: CacheManager

    // Assuming we can mock or spy MetricsService to verify interactions,
    // or just check the meter registry directly. Let's use the registry.
    @Autowired private lateinit var meterRegistry: io.micrometer.core.instrument.MeterRegistry

    @Test
    fun `property - fetching non-existent item records a cache miss`() {
        val cache = cacheManager.getCache("postBySlug")
        requireNotNull(cache) { "postBySlug cache should exist" }

        val startMisses = meterRegistry.counter("cache.misses")?.count() ?: 0.0

        cache.get("non-existent-key")

        val endMisses = meterRegistry.counter("cache.misses")?.count() ?: 0.0
        require(endMisses > startMisses) {
            "Cache miss was not recorded. Start: \$startMisses, End: \$endMisses"
        }
    }

    @Test
    fun `property - fetching existent item records a cache hit`() {
        val cache = cacheManager.getCache("postBySlug")
        requireNotNull(cache) { "postBySlug cache should exist" }

        cache.put("existent-key", "some-value")

        val startHits = meterRegistry.counter("cache.hits")?.count() ?: 0.0

        cache.get("existent-key")

        val endHits = meterRegistry.counter("cache.hits")?.count() ?: 0.0
        require(endHits > startHits) {
            "Cache hit was not recorded. Start: \$startHits, End: \$endHits"
        }
    }
}
