package br.dev.demoraes.abrolhos.infrastructure.monitoring.health

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import javax.sql.DataSource

/**
 * Custom health indicator for the PostgreSQL database connection.
 *
 * Executes a lightweight `SELECT 1` query and measures the response time, reporting both the
 * database type and latency in the health details.
 *
 * This check is registered under the `databaseHealth` component name and helps accurately report
 * database latency beyond Spring Boot's default health auto-configuration.
 */
@Component("databaseHealth")
class DatabaseHealthIndicator(private val dataSource: DataSource) : HealthIndicator {

    /**
     * Executes the check and maps latency into the details payload.
     * @return [Health] indicating UP with latency, or DOWN with error details.
     */
    override fun health(): Health {
        return try {
            dataSource.connection.use { conn ->
                val start = System.currentTimeMillis()
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT 1").use { rs -> rs.next() }
                }
                val duration = System.currentTimeMillis() - start

                Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("responseTime", "${duration}ms")
                    .build()
            }
        } catch (e: Exception) {
            Health.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.message ?: "Unknown error")
                .build()
        }
    }
}
