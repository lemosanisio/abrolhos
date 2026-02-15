package br.dev.demoraes.abrolhos.domain.exceptions

/**
 * Exception thrown when encryption or decryption operations fail.
 *
 * This exception is used to wrap cryptographic errors and provide
 * meaningful error messages for encryption-related failures.
 *
 * Requirement 3.6: Error handling for encryption operations
 */
class EncryptionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
