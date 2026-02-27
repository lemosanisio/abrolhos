package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.domain.entities.Invite
import br.dev.demoraes.abrolhos.domain.entities.InviteToken
import br.dev.demoraes.abrolhos.domain.entities.PasswordHash
import br.dev.demoraes.abrolhos.domain.entities.PlaintextPassword
import br.dev.demoraes.abrolhos.domain.entities.Role
import br.dev.demoraes.abrolhos.domain.entities.TotpCode
import br.dev.demoraes.abrolhos.domain.entities.TotpSecret
import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.repository.InviteRepository
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import ulid.ULID
import java.time.OffsetDateTime
import java.util.Locale

/**
 * Property-based test for persisted secret usage during account activation.
 *
 * **Property 5: Persisted Secret Usage in Activation** **Validates: Requirements 4.2**
 *
 * This test verifies that activateAccount() uses the persisted secret from the invite for TOTP
 * verification, not a client-provided secret.
 *
 * Property: ∀ invite with persisted secret, ∀ valid TOTP code.
 * ```
 *           activateAccount(invite, password, code) → verifies code against invite.totpSecret
 * ```
 */
class PersistedSecretUsagePropertyTest {

    /**
     * Helper function to generate a valid Base32 string from an integer. Base32 alphabet: A-Z (26
     * chars) + 2-7 (6 chars) = 32 chars
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

    private val testPassword = PlaintextPassword("Test@1234Abc!")
    private val testHash =
        PasswordHash("\$2a\$12\$abcdefghijklmnopqrstuvuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu")

    private fun buildAuthService(
        userRepository: UserRepository,
        inviteRepository: InviteRepository,
        totpService: TotpService,
        tokenService: TokenService,
        passwordService: PasswordService,
    ) =
        AuthService(
            userRepository,
            inviteRepository,
            totpService,
            passwordService,
            tokenService,
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
        )

    /**
     * Property: For any invite with a persisted secret and any valid TOTP code, activateAccount
     * should verify the code against the persisted secret from the invite, not against any
     * client-provided secret.
     *
     * This property verifies that:
     * 1. The persisted secret from the invite is used for verification
     * 2. The TOTP code is verified against the persisted secret
     * 3. The user is activated with the persisted secret
     * 4. The invite is deleted after successful activation
     */
    @Test
    fun `property - activateAccount uses persisted secret from invite for verification`() {
        repeat(100) {
            // Given: Create test dependencies
            val userRepository: UserRepository = mockk()
            val inviteRepository: InviteRepository = mockk()
            val totpService: TotpService = mockk()
            val tokenService: TokenService = mockk()
            val passwordService: PasswordService = mockk(relaxed = true)
            val authService =
                buildAuthService(
                    userRepository,
                    inviteRepository,
                    totpService,
                    tokenService,
                    passwordService
                )

            // Given: Create an invite with a persisted secret
            val token = InviteToken("a".repeat(64))
            val userId = ULID.nextULID()
            val persistedSecret = TotpSecret("PER${generateBase32Suffix(it)}")
            val invite =
                Invite(
                    id = ULID.nextULID(),
                    token = token,
                    userId = userId,
                    expiryDate = OffsetDateTime.now().plusDays(1),
                    createdAt = OffsetDateTime.now(),
                    totpSecret = persistedSecret // Secret is persisted
                )

            // Given: Create an inactive user
            val username = "testuser$it"
            val user =
                User(
                    id = userId,
                    username = Username(username),
                    totpSecret = null,
                    passwordHash = null,
                    isActive = false,
                    role = Role.USER,
                    createdAt = OffsetDateTime.now(),
                    updatedAt = OffsetDateTime.now()
                )

            // Given: Create a valid TOTP code
            val totpCode = TotpCode(String.format(Locale.US, "%06d", it % 1000000))

            // Given: Setup mocks
            every { inviteRepository.findByToken(token) } returns invite
            every { userRepository.findById(userId) } returns user
            every { passwordService.validatePassword(testPassword) } returns emptyList()
            every { passwordService.hashPassword(testPassword) } returns testHash
            every { totpService.verifyCode(persistedSecret, totpCode) } returns true
            every { userRepository.save(any()) } answers { firstArg() }
            every { inviteRepository.deleteById(invite.id) } returns Unit
            every { tokenService.generateToken(any()) } returns "jwt-token-$it"

            // When: Activate the account
            val jwtToken = authService.activateAccount(token, testPassword, totpCode)

            // Then: The TOTP code should be verified against the persisted secret
            verify(exactly = 1) { totpService.verifyCode(persistedSecret, totpCode) }

            // Then: The user should be saved with the persisted secret and password hash
            verify(exactly = 1) {
                userRepository.save(
                    match { savedUser ->
                        savedUser.totpSecret == persistedSecret &&
                            savedUser.passwordHash == testHash &&
                            savedUser.isActive &&
                            savedUser.id == userId
                    }
                )
            }

            // Then: The invite should be deleted
            verify(exactly = 1) { inviteRepository.deleteById(invite.id) }

            // Then: A JWT token should be generated
            jwtToken shouldBe "jwt-token-$it"
        }
    }

    /**
     * Property: For any invite without a persisted secret, activateAccount should throw an
     * IllegalStateException and not proceed with activation.
     *
     * This property verifies that:
     * 1. If the invite has no persisted secret, an exception is thrown
     * 2. The user is NOT activated
     * 3. The invite is NOT deleted
     * 4. No JWT token is generated
     */
    @Test
    fun `property - activateAccount throws exception when invite has no persisted secret`() {
        repeat(100) {
            // Given: Create test dependencies
            val userRepository: UserRepository = mockk()
            val inviteRepository: InviteRepository = mockk()
            val totpService: TotpService = mockk()
            val tokenService: TokenService = mockk()
            val passwordService: PasswordService = mockk(relaxed = true)
            val authService =
                buildAuthService(
                    userRepository,
                    inviteRepository,
                    totpService,
                    tokenService,
                    passwordService
                )

            // Given: Create an invite WITHOUT a persisted secret
            val token = InviteToken("b".repeat(64))
            val userId = ULID.nextULID()
            val invite =
                Invite(
                    id = ULID.nextULID(),
                    token = token,
                    userId = userId,
                    expiryDate = OffsetDateTime.now().plusDays(1),
                    createdAt = OffsetDateTime.now(),
                    totpSecret = null // No secret persisted
                )

            // Given: Create an inactive user
            val username = "testuser$it"
            val user =
                User(
                    id = userId,
                    username = Username(username),
                    totpSecret = null,
                    passwordHash = null,
                    isActive = false,
                    role = Role.USER,
                    createdAt = OffsetDateTime.now(),
                    updatedAt = OffsetDateTime.now()
                )

            // Given: Create a TOTP code
            val totpCode = TotpCode(String.format(Locale.US, "%06d", it % 1000000))

            // Given: Setup mocks
            every { inviteRepository.findByToken(token) } returns invite
            every { userRepository.findById(userId) } returns user
            every { passwordService.validatePassword(testPassword) } returns emptyList()
            every { passwordService.hashPassword(testPassword) } returns testHash

            // When/Then: Activating the account should throw an exception
            val exception =
                org.junit.jupiter.api.assertThrows<IllegalStateException> {
                    authService.activateAccount(token, testPassword, totpCode)
                }

            // Then: The exception message should indicate the missing secret
            exception.message shouldBe
                "TOTP secret not found in invite. Invite must be validated first."

            // Then: The user should NOT be saved
            verify(exactly = 0) { userRepository.save(any()) }

            // Then: The invite should NOT be deleted
            verify(exactly = 0) { inviteRepository.deleteById(any()) }

            // Then: No JWT token should be generated
            verify(exactly = 0) { tokenService.generateToken(any()) }
        }
    }

    /**
     * Property: For any invite with a persisted secret and an invalid TOTP code, activateAccount
     * should verify against the persisted secret and throw an exception when verification fails.
     *
     * This property verifies that:
     * 1. The persisted secret is used for verification
     * 2. When verification fails, an InvalidTotpCodeException is thrown
     * 3. The user is NOT activated
     * 4. The invite is NOT deleted
     */
    @Test
    fun `property - activateAccount verifies against persisted secret and fails with invalid code`() {
        repeat(100) {
            // Given: Create test dependencies
            val userRepository: UserRepository = mockk()
            val inviteRepository: InviteRepository = mockk()
            val totpService: TotpService = mockk()
            val tokenService: TokenService = mockk()
            val passwordService: PasswordService = mockk(relaxed = true)
            val authService =
                buildAuthService(
                    userRepository,
                    inviteRepository,
                    totpService,
                    tokenService,
                    passwordService
                )

            // Given: Create an invite with a persisted secret
            val token = InviteToken("c".repeat(64))
            val userId = ULID.nextULID()
            val persistedSecret = TotpSecret("PER${generateBase32Suffix(it)}")
            val invite =
                Invite(
                    id = ULID.nextULID(),
                    token = token,
                    userId = userId,
                    expiryDate = OffsetDateTime.now().plusDays(1),
                    createdAt = OffsetDateTime.now(),
                    totpSecret = persistedSecret
                )

            // Given: Create an inactive user
            val username = "testuser$it"
            val user =
                User(
                    id = userId,
                    username = Username(username),
                    totpSecret = null,
                    passwordHash = null,
                    isActive = false,
                    role = Role.USER,
                    createdAt = OffsetDateTime.now(),
                    updatedAt = OffsetDateTime.now()
                )

            // Given: Create an invalid TOTP code
            val invalidTotpCode = TotpCode(String.format(Locale.US, "%06d", it % 1000000))

            // Given: Setup mocks - verification fails
            every { inviteRepository.findByToken(token) } returns invite
            every { userRepository.findById(userId) } returns user
            every { passwordService.validatePassword(testPassword) } returns emptyList()
            every { passwordService.hashPassword(testPassword) } returns testHash
            every { totpService.verifyCode(persistedSecret, invalidTotpCode) } returns false

            // When/Then: Activating the account should throw an exception
            val exception =
                org.junit.jupiter.api.assertThrows<
                    br.dev.demoraes.abrolhos.domain.exceptions.InvalidTotpCodeException
                    > {
                    authService.activateAccount(token, testPassword, invalidTotpCode)
                }

            // Then: The exception message should indicate invalid code
            exception.message shouldBe "Invalid TOTP code"

            // Then: The TOTP code should be verified against the persisted secret
            verify(exactly = 1) { totpService.verifyCode(persistedSecret, invalidTotpCode) }

            // Then: The user should NOT be saved
            verify(exactly = 0) { userRepository.save(any()) }

            // Then: The invite should NOT be deleted
            verify(exactly = 0) { inviteRepository.deleteById(any()) }

            // Then: No JWT token should be generated
            verify(exactly = 0) { tokenService.generateToken(any()) }
        }
    }
}
