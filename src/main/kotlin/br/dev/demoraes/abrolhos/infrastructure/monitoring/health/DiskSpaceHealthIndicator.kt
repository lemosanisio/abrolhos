package br.dev.demoraes.abrolhos.infrastructure.monitoring.health

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import java.io.File

/**
 * Custom health indicator for disk space monitoring.
 *
 * Reports the application as DOWN if the free disk space on the current working directory's
 * filesystem drops below the configured threshold (1 GB).
 */
@Component("diskSpaceHealth")
class DiskSpaceHealthIndicator : HealthIndicator {

    companion object {
        /** Minimum free space threshold required (1 GB). */
        private const val THRESHOLD_BYTES: Long = 1L * 1024 * 1024 * 1024 // 1 GB

        /** Conversion factor to Megabytes for logging/metrics. */
        private const val BYTES_PER_MB: Long = 1024 * 1024
    }

    /**
     * Checks if the filesystem where the application is running has sufficient disk space.
     * @return [Health] indicating UP if free space > 1GB, or DOWN otherwise with details.
     */
    override fun health(): Health {
        val file = File(".")
        val freeSpace = file.freeSpace
        val totalSpace = file.totalSpace
        val usedSpace = totalSpace - freeSpace
        val usedPercent =
            if (totalSpace > 0) (usedSpace.toDouble() / totalSpace * 100).toInt() else 0

        val details =
            mapOf(
                "free" to "${freeSpace / BYTES_PER_MB}MB",
                "total" to "${totalSpace / BYTES_PER_MB}MB",
                "used" to "$usedPercent%",
                "threshold" to "${THRESHOLD_BYTES / BYTES_PER_MB}MB"
            )

        return if (freeSpace > THRESHOLD_BYTES) {
            Health.up().withDetails(details).build()
        } else {
            Health.down()
                .withDetails(details)
                .withDetail("error", "Free disk space below threshold")
                .build()
        }
    }
}
