package br.dev.demoraes.abrolhos.infrastructure.web.dto.response

/**
 * Response DTO for server time diagnostic endpoint.
 * Provides information about system time, NTP time, and clock skew for debugging TOTP issues.
 *
 * @property systemTimeMillis Current system time in epoch milliseconds
 * @property ntpTimeMillis Current NTP server time in epoch milliseconds
 * @property skewMillis Time difference between system and NTP time in milliseconds
 * @property skewSeconds Time difference between system and NTP time in seconds
 * @property isAcceptable Whether the time skew is within acceptable limits (< 5 seconds)
 */
data class ServerTimeResponse(
    val systemTimeMillis: Long,
    val ntpTimeMillis: Long,
    val skewMillis: Long,
    val skewSeconds: Double,
    val isAcceptable: Boolean
)
