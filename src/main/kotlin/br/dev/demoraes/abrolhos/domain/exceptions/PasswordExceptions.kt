package br.dev.demoraes.abrolhos.domain.exceptions

/**
 * Sealed exception hierarchy for all password-related errors.
 *
 * Using a sealed class ensures exhaustive handling at all call sites.
 */
sealed class PasswordException(message: String) : RuntimeException(message)

/** Thrown when a supplied plaintext password does not match the stored hash. */
class InvalidPasswordException(message: String) : PasswordException(message)

/** Generic error occurred in the password reset flow. */
class PasswordResetException(message: String) : PasswordException(message)

/**
 * Thrown when a new password fails the configured password policy.
 *
 * @property violations Human-readable list of which policy rules were not satisfied
 */
class PasswordPolicyViolationException(val violations: List<String>) :
        PasswordException(
                "Password does not meet policy requirements: ${violations.joinToString(", ")}"
        )

/** Thrown when a password reset token has passed its expiration time. */
class PasswordResetTokenExpiredException : PasswordException("Password reset token has expired")

/** Thrown when a password reset token cannot be found in the repository. */
class PasswordResetTokenNotFoundException : PasswordException("Invalid password reset token")
