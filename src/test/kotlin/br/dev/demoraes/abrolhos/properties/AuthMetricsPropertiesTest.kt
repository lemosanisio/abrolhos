package br.dev.demoraes.abrolhos.properties

import br.dev.demoraes.abrolhos.IntegrationTestBase
import br.dev.demoraes.abrolhos.infrastructure.monitoring.MetricsService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
class AuthMetricsPropertiesTest : IntegrationTestBase() {

    @Autowired private lateinit var metricsService: MetricsService

    @Autowired private lateinit var meterRegistry: io.micrometer.core.instrument.MeterRegistry

    @Test
    fun `property - login failures record specific outcome tags`() {
        val outcomes =
                listOf(
                        "invalid_password",
                        "invalid_user",
                        "rate_limited",
                        "no_totp_secret",
                        "invalid_totp",
                        "inactive_user"
                )

        outcomes.forEach { outcome ->
            val startCount =
                    meterRegistry.counter("auth.login.failure", "outcome", outcome)?.count() ?: 0.0

            metricsService.recordLoginFailure(outcome)

            val endCount =
                    meterRegistry.counter("auth.login.failure", "outcome", outcome)?.count() ?: 0.0
            require(endCount > startCount) {
                "Metric auth.login.failure with outcome=\$outcome was not incremented"
            }
        }
    }

    @Test
    fun `property - login success records metric`() {
        val startCount = meterRegistry.counter("auth.login.success")?.count() ?: 0.0

        metricsService.recordLoginSuccess()

        val endCount = meterRegistry.counter("auth.login.success")?.count() ?: 0.0
        require(endCount > startCount) { "Metric auth.login.success was not incremented" }
    }
}
