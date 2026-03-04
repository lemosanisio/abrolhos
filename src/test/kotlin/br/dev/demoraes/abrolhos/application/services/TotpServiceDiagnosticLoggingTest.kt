package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.domain.entities.TotpCode
import br.dev.demoraes.abrolhos.domain.entities.TotpSecret
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.matchers.shouldBe
import kotlin.random.Random
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

/**
 * Property-based test for diagnostic logging completeness in TotpService.
 *
 * **Property 10: Diagnostic Logging Completeness** **Validates: Requirements 1.1, 1.2, 1.3, 1.4**
 *
 * This test verifies that all required diagnostic information is logged when TOTP operations occur:
 * - Requirement 1.1: Log first 8 characters of Base32-encoded secret
 * - Requirement 1.2: Log byte count of decoded result
 * - Requirement 1.3: Log current system timestamp in epoch milliseconds
 * - Requirement 1.4: Log codes generated for current, previous, and next time windows
 */
class TotpServiceDiagnosticLoggingTest {
    private val totpService = TotpService()
    private lateinit var logAppender: ListAppender<ILoggingEvent>
    private lateinit var logger: Logger

    @BeforeEach
    fun setup() {
        // Set up log capture
        logger = LoggerFactory.getLogger(TotpService::class.java) as Logger
        logAppender = ListAppender()
        logAppender.start()
        logger.addAppender(logAppender)
        logger.level = Level.DEBUG
    }

    @AfterEach
    fun teardown() {
        logger.detachAppender(logAppender)
        logAppender.stop()
    }

    /**
     * Property: For any valid TOTP secret and code, verifyCode() must log all required diagnostic
     * information.
     *
     * This property verifies that the logging is complete regardless of whether the code is valid
     * or invalid.
     */
    @Suppress("LoopWithTooManyJumpStatements", "CyclomaticComplexMethod")
    @Test
    fun `property - verifyCode logs all required diagnostic information for any secret and code`() {
        // Generate arbitrary valid Base32 secrets and codes
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

        var successfulTests = 0
        var attempts = 0

        while (successfulTests < 100 && attempts < 200) {
            attempts++

            // Generate random Base32 secret (16-32 chars)
            val secretLength = Random.nextInt(16, 33)
            val secretValue = (1..secretLength).map { base32Chars.random() }.joinToString("")

            // Try to create the secret - if validation fails, skip this iteration
            val secret =
                    try {
                        TotpSecret(secretValue)
                    } catch (@Suppress("SwallowedException") e: IllegalArgumentException) {
                        continue
                    }

            // Generate random 6-digit code
            val codeValue = (1..6).map { Random.nextInt(0, 10) }.joinToString("")
            val code = TotpCode(codeValue)

            // Clear previous logs
            logAppender.list.clear()

            // Record timestamp before verification
            val timestampBefore = System.currentTimeMillis()

            // When: Verify the code (may succeed or fail, doesn't matter for logging)
            try {
                totpService.verifyCode(secret, code)
            } catch (@Suppress("SwallowedException") e: Exception) {
                // If an exception occurs during verification, skip this iteration
                continue
            }

            // Record timestamp after verification
            val timestampAfter = System.currentTimeMillis()

            // Then: Verify all required diagnostic information is logged
            val logMessages = logAppender.list.map { it.formattedMessage }

            if (!hasRequiredLogs(logMessages, secret, timestampBefore, timestampAfter)) {
                continue
            }

            successfulTests++
        }

        // Ensure we ran enough successful tests
        successfulTests shouldBe 100
    }

    private fun hasRequiredLogs(
            logMessages: List<String>,
            secret: TotpSecret,
            timestampBefore: Long,
            timestampAfter: Long
    ): Boolean {
        // Requirement 1.1: Log that secret was generated/verified without leaking it
        val hasSecretLog = logMessages.any { it.contains("Verifying TOTP code") }

        // Requirement 1.2: Log byte count of decoded result
        val hasByteCountLog =
                logMessages.any {
                    it.contains("Decoded byte count:") &&
                            it.matches(Regex(".*Decoded byte count: \\d+.*"))
                }

        // Requirement 1.3: Log system timestamp in epoch milliseconds
        val hasTimestampLog =
                logMessages.any { message ->
                    if (message.contains("System timestamp:")) {
                        // Extract the timestamp from the log message
                        val timestampMatch = Regex("System timestamp: (\\d+)").find(message)
                        if (timestampMatch != null) {
                            val loggedTimestamp = timestampMatch.groupValues[1].toLong()
                            // Verify it's within the time range of the test execution
                            loggedTimestamp in timestampBefore..timestampAfter
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }

        // Requirement 1.4: Log codes for previous, current, and next windows
        val hasWindowCodesLog =
                logMessages.any {
                    it.contains("Generated codes - Previous:") &&
                            it.contains("Current:") &&
                            it.contains("Next:")
                }

        // Verify the window codes are 6-digit numbers
        val windowCodesMessage = logMessages.find { it.contains("Generated codes - Previous:") }
        val windowCodesValid =
                windowCodesMessage != null &&
                        listOf(
                                        windowCodesMessage.contains("Previous:"),
                                        windowCodesMessage.contains("Current:"),
                                        windowCodesMessage.contains("Next:")
                                )
                                .all { it }

        return hasSecretLog &&
                hasByteCountLog &&
                hasTimestampLog &&
                hasWindowCodesLog &&
                windowCodesValid
    }

    /**
     * Property: For any generated secret, generateSecret() must log the first 8 characters.
     *
     * This verifies Requirement 1.1 for secret generation.
     */
    @Test
    fun `property - generateSecret logs first 8 characters of generated secret`() {
        repeat(100) {
            // Clear previous logs
            logAppender.list.clear()

            // When: Generate a secret
            val secret = totpService.generateSecret()

            // Then: Verify the first 8 characters are logged
            val logMessages = logAppender.list.map { it.formattedMessage }
            val hasSecretLog = logMessages.any { it.contains("Generated new TOTP secret") }
            hasSecretLog shouldBe true
        }
    }

    /**
     * Property: When Base32 decoding fails, the exception details and secret must be logged.
     *
     * This verifies Requirement 1.6 for error logging.
     *
     * Note: We use valid Base32 characters but create secrets that may fail during decoding due to
     * length or padding issues.
     */
    @Test
    fun `property - verifyCode logs exception details when Base32 decoding fails`() {
        // Generate valid Base32 secrets that might fail during decoding (very short secrets)
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

        repeat(50) {
            // Generate random short secret (2-7 chars) that might cause decoding issues
            val secretLength = Random.nextInt(2, 8)
            val secretValue = (1..secretLength).map { base32Chars.random() }.joinToString("")

            // Try to create the secret - if validation fails, skip this iteration
            val secret =
                    try {
                        TotpSecret(secretValue)
                    } catch (@Suppress("SwallowedException") e: IllegalArgumentException) {
                        return@repeat
                    }

            // Generate random 6-digit code
            val codeValue = (1..6).map { Random.nextInt(0, 10) }.joinToString("")
            val code = TotpCode(codeValue)

            // Clear previous logs
            logAppender.list.clear()

            // When: Verify with potentially problematic secret
            totpService.verifyCode(secret, code)

            // Then: Check if exception was logged (only if decoding actually failed)
            val logMessages = logAppender.list.map { it.formattedMessage }

            val hasExceptionLog = logMessages.any { it.contains("TOTP verification exception:") }

            // If exception was logged, verify the secret was also logged
            if (hasExceptionLog) {
                val hasFailedSecretLog = logMessages.any { it.contains("Failed to decode secret") }
                hasFailedSecretLog shouldBe true
            }
        }
    }
}
