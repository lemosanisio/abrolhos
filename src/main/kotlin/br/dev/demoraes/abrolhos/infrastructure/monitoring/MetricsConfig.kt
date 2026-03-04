package br.dev.demoraes.abrolhos.infrastructure.monitoring

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Micrometer metrics configuration.
 *
 * Adds common tags to all metrics so they can be easily filtered in VictoriaMetrics or any
 * Prometheus-compatible backend.
 */
@Configuration
class MetricsConfig {

    @Bean
    fun metricsCommonTags(): MeterRegistryCustomizer<MeterRegistry> {
        return MeterRegistryCustomizer { registry ->
            registry.config().commonTags("application", "abrolhos")
        }
    }
}
