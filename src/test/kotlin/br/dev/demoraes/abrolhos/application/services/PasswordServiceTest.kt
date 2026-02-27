package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.application.audit.AuditLogger
import br.dev.demoraes.abrolhos.application.config.PasswordProperties
import br.dev.demoraes.abrolhos.domain.entities.PasswordResetToken
import br.dev.demoraes.abrolhos.domain.entities.PlaintextPassword
import br.dev.demoraes.abrolhos.domain.entities.Role
import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.exceptions.InvalidPasswordException
import br.dev.demoraes.abrolhos.domain.exceptions.PasswordPolicyViolationException
import br.dev.demoraes.abrolhos.domain.exceptions.PasswordResetTokenExpiredException
import br.dev.demoraes.abrolhos.domain.exceptions.PasswordResetTokenNotFoundException
import br.dev.demoraes.abrolhos.domain.repository.PasswordResetTokenRepository
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.security.SecureRandom
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import ulid.ULID

/**
 * Unit tests for [PasswordService].
 *
 * Uses BCryptPasswordEncoder with strength 4 so hashing is fast in tests, and all other
 * dependencies are mocked.
 */
class PasswordServiceTest {

    private val properties =
            PasswordProperties(
                    minLength = 12,
                    maxLength = 128,
                    requireUppercase = true,
                    requireLowercase = true,
                    requireDigit = true,
                    requireSpecialChar = true,
                    bcryptStrength = 4, // fast for tests
                    specialChars = "!@#\$%^&*()_+-=[]{}|;:,.<>?",
                    resetTokenExpiryHours = 1,
                    resetTokenByteSize = 32,
            )

    private val encoder = BCryptPasswordEncoder(4)
    private val tokenRepository: PasswordResetTokenRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val auditLogger: AuditLogger = mockk(relaxed = true)
    private val rateLimitService: RateLimitService = mockk()
    private val meterRegistry = SimpleMeterRegistry()
    private val secureRandom = SecureRandom()

    private val service =
            PasswordService(
                    encoder,
                    tokenRepository,
                    userRepository,
                    auditLogger,
                    rateLimitService,
                    meterRegistry,
                    properties,
                    secureRandom,
            )

    // ---------------------------------------------------------------------------
    // validatePassword tests
    // ---------------------------------------------------------------------------

    @Test
    fun `validatePassword returns empty list for valid password`() {
        val result = service.validatePassword(PlaintextPassword("ValidP@ss1!A"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `validatePassword flags short passwords`() {
        val result = service.validatePassword(PlaintextPassword("Short@1!"))
        assertTrue(result.any { it.contains("at least 12 characters") })
    }

    @Test
    fun `validatePassword flags missing uppercase`() {
        val result = service.validatePassword(PlaintextPassword("nouppercase1!abc"))
        assertTrue(result.any { it.contains("uppercase") })
    }

    @Test
    fun `validatePassword flags missing lowercase`() {
        val result = service.validatePassword(PlaintextPassword("NOLOWERCASE1!ABC"))
        assertTrue(result.any { it.contains("lowercase") })
    }

    @Test
    fun `validatePassword flags missing digit`() {
        val result = service.validatePassword(PlaintextPassword("NoDigits!!Aabb"))
        assertTrue(result.any { it.contains("digit") })
    }

    @Test
    fun `validatePassword flags missing special character`() {
        val result = service.validatePassword(PlaintextPassword("NoSpecialChar123Abc"))
        assertTrue(result.any { it.contains("special character") })
    }

    @Test
    fun `validatePassword can report multiple violations`() {
        val result = service.validatePassword(PlaintextPassword("short"))
        assertTrue(result.size > 1) // multiple rules violated
    }

    // ---------------------------------------------------------------------------
    // hashPassword tests
    // ---------------------------------------------------------------------------

    @Test
    fun `hashPassword returns bcrypt hash for valid password`() {
        val hash = service.hashPassword(PlaintextPassword("ValidP@ssw0rd!"))
        assertNotNull(hash)
        assertTrue(hash.value.startsWith("\$2a\$"))
    }

    @Test
    fun `hashPassword throws PasswordPolicyViolationException for weak password`() {
        val ex =
                assertThrows<PasswordPolicyViolationException> {
                    service.hashPassword(PlaintextPassword("weak"))
                }
        assertTrue(ex.violations.isNotEmpty())
    }

    // ---------------------------------------------------------------------------
    // verifyPassword tests
    // ---------------------------------------------------------------------------

    @Test
    fun `verifyPassword returns true for matching password`() {
        val password = PlaintextPassword("ValidP@ssw0rd!")
        val hash = service.hashPassword(password)
        assertTrue(service.verifyPassword(password, hash))
    }

    @Test
    fun `verifyPassword returns false for wrong password`() {
        val hash = service.hashPassword(PlaintextPassword("ValidP@ssw0rd!"))
        assertFalse(service.verifyPassword(PlaintextPassword("Wr0ngP@ssword!"), hash))
    }

    // ---------------------------------------------------------------------------
    // changePassword tests
    // ---------------------------------------------------------------------------

    @Test
    fun `changePassword succeeds with correct current password and valid new password`() {
        val currentPassword = PlaintextPassword("OldP@ssw0rd123!")
        val newPassword = PlaintextPassword("NewP@ssw0rd456!")
        val currentHash = service.hashPassword(currentPassword)
        val userId = ULID.parseULID(ULID.randomULID())

        every { rateLimitService.tryConsume(any(), any()) } returns
                mockk { every { isAllowed } returns true }

        val newHash = service.changePassword(userId, currentPassword, newPassword, currentHash)

        assertTrue(newHash.value.startsWith("\$2a\$"))
        assertTrue(service.verifyPassword(newPassword, newHash))
    }

    @Test
    fun `changePassword throws InvalidPasswordException for wrong current password`() {
        val currentPassword = PlaintextPassword("OldP@ssw0rd123!")
        val wrongPassword = PlaintextPassword("Wr0ngP@ssword!")
        val currentHash = service.hashPassword(currentPassword)
        val userId = ULID.parseULID(ULID.randomULID())

        every { rateLimitService.tryConsume(any(), any()) } returns
                mockk { every { isAllowed } returns true }

        assertThrows<InvalidPasswordException> {
            service.changePassword(
                    userId,
                    wrongPassword,
                    PlaintextPassword("NewP@ssw0rd456!"),
                    currentHash
            )
        }
    }

    @Test
    fun `changePassword throws InvalidPasswordException when new password equals current`() {
        val password = PlaintextPassword("SameP@ssw0rd123!")
        val hash = service.hashPassword(password)
        val userId = ULID.parseULID(ULID.randomULID())

        every { rateLimitService.tryConsume(any(), any()) } returns
                mockk { every { isAllowed } returns true }

        assertThrows<InvalidPasswordException> {
            service.changePassword(userId, password, password, hash)
        }
    }

    // ---------------------------------------------------------------------------
    // generateResetToken tests
    // ---------------------------------------------------------------------------

    @Test
    fun `generateResetToken returns 64-character hex token`() {
        val userId = ULID.parseULID(ULID.randomULID())
        val username = Username("testuser")

        every { rateLimitService.tryConsume(any(), any()) } returns
                mockk { every { isAllowed } returns true }
        every { tokenRepository.deleteByUserId(any()) } returns Unit
        every { tokenRepository.save(any()) } answers { firstArg() }

        val token = service.generateResetToken(userId, username)

        assertEquals(64, token.value.length)
        assertTrue(token.value.matches(Regex("^[a-f0-9]{64}$")))
        verify { tokenRepository.deleteByUserId(userId) }
        verify { tokenRepository.save(any()) }
    }

    // ---------------------------------------------------------------------------
    // resetPassword tests
    // ---------------------------------------------------------------------------

    @Test
    fun `resetPassword throws PasswordResetTokenNotFoundException for unknown token`() {
        val token = PasswordResetToken("a".repeat(64))
        every { tokenRepository.findByToken(token) } returns null

        assertThrows<PasswordResetTokenNotFoundException> {
            service.resetPassword(token, PlaintextPassword("NewP@ssw0rd456!"))
        }
    }

    @Test
    fun `resetPassword throws PasswordResetTokenExpiredException for expired token`() {
        val token = PasswordResetToken("b".repeat(64))
        val expiredEntity =
                br.dev.demoraes.abrolhos.domain.entities.PasswordResetTokenEntity(
                        id = ULID.parseULID(ULID.randomULID()),
                        userId = ULID.parseULID(ULID.randomULID()),
                        token = token,
                        expiresAt = OffsetDateTime.now().minusHours(2),
                        createdAt = OffsetDateTime.now().minusHours(3),
                )

        every { tokenRepository.findByToken(token) } returns expiredEntity
        every { tokenRepository.deleteById(any()) } returns Unit

        assertThrows<PasswordResetTokenExpiredException> {
            service.resetPassword(token, PlaintextPassword("NewP@ssw0rd456!"))
        }

        verify { tokenRepository.deleteById(expiredEntity.id) }
    }

    @Test
    fun `resetPassword successfully updates user password for valid token`() {
        val token = PasswordResetToken("c".repeat(64))
        val userId = ULID.parseULID(ULID.randomULID())
        val validEntity =
                br.dev.demoraes.abrolhos.domain.entities.PasswordResetTokenEntity(
                        id = ULID.parseULID(ULID.randomULID()),
                        userId = userId,
                        token = token,
                        expiresAt = OffsetDateTime.now().plusHours(1),
                        createdAt = OffsetDateTime.now(),
                )
        val user =
                User(
                        id = userId,
                        username = Username("testuser"),
                        totpSecret = null,
                        passwordHash = null,
                        isActive = true,
                        role = Role.USER,
                        createdAt = OffsetDateTime.now(),
                        updatedAt = OffsetDateTime.now(),
                )

        every { tokenRepository.findByToken(token) } returns validEntity
        every { userRepository.findById(userId) } returns user
        every { userRepository.save(any()) } returns user
        every { tokenRepository.deleteById(any()) } returns Unit

        val returnedUserId = service.resetPassword(token, PlaintextPassword("NewP@ssw0rd456!"))

        assertEquals(userId, returnedUserId)
        verify { userRepository.save(match { it.passwordHash != null }) }
        verify { tokenRepository.deleteById(validEntity.id) }
    }
}
