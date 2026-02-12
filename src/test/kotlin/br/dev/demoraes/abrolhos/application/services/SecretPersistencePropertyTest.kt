package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.domain.entities.Invite
import br.dev.demoraes.abrolhos.domain.entities.InviteToken
import br.dev.demoraes.abrolhos.domain.entities.Role
import br.dev.demoraes.abrolhos.domain.entities.TotpSecret
import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.repository.InviteRepository
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import ulid.ULID
import java.time.OffsetDateTime

/**
 * Property-based test for secret persistence during invite validation.
 *
 * **Property 4: Secret Persistence on Invite Validation**
 * **Validates: Requirements 4.1**
 *
 * This test verifies that when an invite is validated without a persisted secret,
 * the system generates a new secret, validates it, and persists it to the database.
 *
 * Property: ∀ invite without secret. validateInvite(invite) → secret is persisted to database
 */
class SecretPersistencePropertyTest {

    /**
     * Helper function to generate a valid Base32 string from an integer.
     * Base32 alphabet: A-Z (26 chars) + 2-7 (6 chars) = 32 chars
     */
    private fun generateBase32Suffix(index: Int): String {
        val base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val suffix = StringBuilder()
        var num = index
        repeat(10) {
            suffix.append(base32Chars[num % 32])
            num /= 32
        }
        return suffix.toString()
    }

    /**
     * Property: For any invite without a persisted secret, validateInvite should generate
     * and persist a new secret to the database.
     *
     * This property verifies that:
     * 1. A new secret is generated when invite has no secret
     * 2. The secret is validated before persistence
     * 3. The secret is persisted to the database via inviteRepository.save()
     * 4. The persisted invite contains the generated secret
     */
    @Test
    fun `property - validateInvite generates and persists secret for invites without one`() {
        repeat(100) {
            // Given: Create test dependencies
            val userRepository: UserRepository = mockk()
            val inviteRepository: InviteRepository = mockk()
            val totpService: TotpService = mockk()
            val tokenService: TokenService = mockk()
            val authService = AuthService(userRepository, inviteRepository, totpService, tokenService)

            // Given: Create an invite without a persisted secret
            val token = InviteToken("a".repeat(64))
            val userId = ULID.nextULID()
            val invite = Invite(
                id = ULID.nextULID(),
                token = token,
                userId = userId,
                expiryDate = OffsetDateTime.now().plusDays(1),
                createdAt = OffsetDateTime.now(),
                totpSecret = null // No secret initially
            )

            // Given: Create an inactive user
            val username = "testuser$it"
            val user = User(
                id = userId,
                username = Username(username),
                totpSecret = null,
                isActive = false,
                role = Role.USER,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )

            // Given: Mock a generated secret (must be valid Base32: A-Z, 2-7)
            val generatedSecret = TotpSecret("GEN${generateBase32Suffix(it)}")
            val provisioningUri = "otpauth://totp/Abrolhos:$username?secret=${generatedSecret.value}&issuer=Abrolhos"

            // Given: Setup mocks
            every { inviteRepository.findByToken(token) } returns invite
            every { userRepository.findById(userId) } returns user
            every { totpService.generateSecret() } returns generatedSecret
            every { totpService.validateSecret(generatedSecret) } returns SecretValidation(
                isValid = true,
                byteCount = 20,
                expectedByteCount = 20
            )
            every { totpService.generateProvisioningUri(username, generatedSecret) } returns provisioningUri

            // Capture the invite that is saved
            val savedInviteSlot = slot<Invite>()
            every { inviteRepository.save(capture(savedInviteSlot)) } answers { savedInviteSlot.captured }

            // When: Validate the invite
            val result = authService.validateInvite(token)

            // Then: A secret should be generated
            verify(exactly = 1) { totpService.generateSecret() }

            // Then: The secret should be validated before persistence
            verify(exactly = 1) { totpService.validateSecret(generatedSecret) }

            // Then: The invite should be saved with the generated secret
            verify(exactly = 1) { inviteRepository.save(any()) }

            // Then: The saved invite should contain the generated secret
            savedInviteSlot.captured.totpSecret shouldNotBe null
            savedInviteSlot.captured.totpSecret shouldBe generatedSecret

            // Then: The result should contain the provisioning URI
            result.username shouldBe username
            result.provisioningUri shouldBe provisioningUri
        }
    }

    /**
     * Property: For any invite with an existing persisted secret, validateInvite should
     * reuse the existing secret without generating a new one or persisting again.
     *
     * This property verifies that:
     * 1. No new secret is generated when invite already has a secret
     * 2. The existing secret is used for provisioning URI generation
     * 3. No save operation is performed (secret already persisted)
     */
    @Test
    fun `property - validateInvite reuses existing secret without generating or persisting`() {
        repeat(100) {
            // Given: Create test dependencies
            val userRepository: UserRepository = mockk()
            val inviteRepository: InviteRepository = mockk()
            val totpService: TotpService = mockk()
            val tokenService: TokenService = mockk()
            val authService = AuthService(userRepository, inviteRepository, totpService, tokenService)

            // Given: Create an invite with an existing persisted secret (must be valid Base32)
            val token = InviteToken("b".repeat(64))
            val userId = ULID.nextULID()
            val existingSecret = TotpSecret("EXT${generateBase32Suffix(it)}")
            val invite = Invite(
                id = ULID.nextULID(),
                token = token,
                userId = userId,
                expiryDate = OffsetDateTime.now().plusDays(1),
                createdAt = OffsetDateTime.now(),
                totpSecret = existingSecret // Secret already exists
            )

            // Given: Create an inactive user
            val username = "testuser$it"
            val user = User(
                id = userId,
                username = Username(username),
                totpSecret = null,
                isActive = false,
                role = Role.USER,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )

            // Given: Setup mocks
            val provisioningUri = "otpauth://totp/Abrolhos:$username?secret=${existingSecret.value}&issuer=Abrolhos"
            every { inviteRepository.findByToken(token) } returns invite
            every { userRepository.findById(userId) } returns user
            every { totpService.generateProvisioningUri(username, existingSecret) } returns provisioningUri

            // When: Validate the invite
            val result = authService.validateInvite(token)

            // Then: The result should use the existing secret
            result.username shouldBe username
            result.provisioningUri shouldBe provisioningUri
        }
    }

    /**
     * Property: For any invite without a secret, if secret validation fails,
     * the system should throw an exception and not persist the invalid secret.
     *
     * This property verifies that:
     * 1. A new secret is generated
     * 2. The secret is validated before persistence
     * 3. If validation fails, an IllegalStateException is thrown
     * 4. The invalid secret is NOT persisted to the database
     */
    @Test
    fun `property - validateInvite throws exception and does not persist invalid secret`() {
        repeat(100) {
            // Given: Create test dependencies
            val userRepository: UserRepository = mockk()
            val inviteRepository: InviteRepository = mockk()
            val totpService: TotpService = mockk()
            val tokenService: TokenService = mockk()
            val authService = AuthService(userRepository, inviteRepository, totpService, tokenService)

            // Given: Create an invite without a persisted secret
            val token = InviteToken("c".repeat(64))
            val userId = ULID.nextULID()
            val invite = Invite(
                id = ULID.nextULID(),
                token = token,
                userId = userId,
                expiryDate = OffsetDateTime.now().plusDays(1),
                createdAt = OffsetDateTime.now(),
                totpSecret = null
            )

            // Given: Create an inactive user
            val username = "testuser$it"
            val user = User(
                id = userId,
                username = Username(username),
                totpSecret = null,
                isActive = false,
                role = Role.USER,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )

            // Given: Mock a generated secret that fails validation (must be valid Base32 for construction)
            val invalidSecret = TotpSecret("INV${generateBase32Suffix(it)}") // Valid Base32 format

            // Given: Setup mocks
            every { inviteRepository.findByToken(token) } returns invite
            every { userRepository.findById(userId) } returns user
            every { totpService.generateSecret() } returns invalidSecret
            every { totpService.validateSecret(invalidSecret) } returns SecretValidation(
                isValid = false,
                byteCount = 5,
                expectedByteCount = 20,
                error = "Secret too short"
            )

            // When/Then: Validating the invite should throw an exception
            val exception = org.junit.jupiter.api.assertThrows<IllegalStateException> {
                authService.validateInvite(token)
            }

            // Then: The exception message should indicate the validation failure
            exception.message shouldBe "Generated TOTP secret is invalid: Secret too short"

            // Then: A secret should be generated
            verify(exactly = 1) { totpService.generateSecret() }

            // Then: The secret should be validated
            verify(exactly = 1) { totpService.validateSecret(invalidSecret) }

            // Then: The invalid secret should NOT be persisted
            verify(exactly = 0) { inviteRepository.save(any()) }
        }
    }
}
