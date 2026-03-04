package br.dev.demoraes.abrolhos.application.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import java.security.SecureRandom

/**
 * Type-safe configuration properties for password policy and security settings.
 *
 * Bound from the `security.password` prefix in application.yml.
 *
 * @property minLength Minimum password length (default 12)
 * @property maxLength Maximum password length (default 128)
 * @property requireUppercase Whether at least one uppercase letter is required
 * @property requireLowercase Whether at least one lowercase letter is required
 * @property requireDigit Whether at least one digit is required
 * @property requireSpecialChar Whether at least one special character is required
 * @property bcryptStrength BCrypt work factor (12-14 recommended)
 * @property specialChars Set of characters considered "special" for policy enforcement
 * @property resetTokenExpiryHours Hours until a password reset token expires (default 1)
 * @property resetTokenByteSize Number of random bytes used to generate a reset token (default 32 →
 * 64 hex chars)
 */
@ConfigurationProperties(prefix = "security.password")
data class PasswordProperties(
    val minLength: Int = 12,
    val maxLength: Int = 128,
    val requireUppercase: Boolean = true,
    val requireLowercase: Boolean = true,
    val requireDigit: Boolean = true,
    val requireSpecialChar: Boolean = true,
    val bcryptStrength: Int = 12,
    val specialChars: String = "!@#\$%^&*()_+-=[]{}|;:,.<>?",
    val resetTokenExpiryHours: Long = 1,
    val resetTokenByteSize: Int = 32,
)

/**
 * Spring configuration that wires the password-related beans.
 *
 * - Creates a [PasswordEncoder] bean backed by BCrypt with the configured work factor.
 * - Creates a [SecureRandom] bean used for token generation (overridable in tests).
 * - Enables scheduled task processing for the token cleanup job.
 */
@Configuration
@EnableConfigurationProperties(PasswordProperties::class)
@EnableScheduling
class PasswordConfiguration {

    @Bean
    fun passwordEncoder(properties: PasswordProperties): PasswordEncoder =
        BCryptPasswordEncoder(properties.bcryptStrength)

    @Bean @ConditionalOnMissingBean
    fun secureRandom(): SecureRandom = SecureRandom()
}
