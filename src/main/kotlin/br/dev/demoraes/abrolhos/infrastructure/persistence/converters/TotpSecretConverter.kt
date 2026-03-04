package br.dev.demoraes.abrolhos.infrastructure.persistence.converters

import br.dev.demoraes.abrolhos.application.services.EncryptionService
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.slf4j.LoggerFactory

/**
 * JPA converter for transparent encryption/decryption of TOTP secrets.
 *
 * This converter automatically encrypts TOTP secrets when persisting to the database and decrypts
 * them when loading from the database, ensuring secrets are never stored in plaintext.
 *
 * Requirements:
 * - 3.1: Encrypt TOTP secrets before database persistence
 * - 3.2: Decrypt TOTP secrets when loading from database
 */
@Converter(autoApply = false)
class TotpSecretConverter(
    private val encryptionService: EncryptionService
) :
    AttributeConverter<String?, String?> {
    /**
     * Converts the TOTP secret to its encrypted database representation.
     *
     * Requirement 3.1: Encrypt using EncryptionService before persistence
     *
     * @param attribute The plaintext TOTP secret from the entity
     * @return The encrypted TOTP secret for database storage, or null if attribute is null
     */
    override fun convertToDatabaseColumn(attribute: String?): String? {
        if (attribute == null) {
            return null
        }

        logger.debug("Encrypting TOTP secret for database storage")
        return encryptionService.encrypt(attribute)
    }

    /**
     * Converts the encrypted database value back to plaintext TOTP secret.
     *
     * Requirement 3.2: Decrypt using EncryptionService when loading
     *
     * @param dbData The encrypted TOTP secret from the database
     * @return The decrypted plaintext TOTP secret, or null if dbData is null
     */
    override fun convertToEntityAttribute(dbData: String?): String? {
        if (dbData == null) {
            return null
        }

        // Strict mode: Fail if decryption fails (no fallback to plaintext)
        logger.debug("Decrypting TOTP secret from database")
        return encryptionService.decrypt(dbData)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TotpSecretConverter::class.java)
    }
}
