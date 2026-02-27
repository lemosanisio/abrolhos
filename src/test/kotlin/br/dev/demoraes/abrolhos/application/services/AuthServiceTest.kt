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
import br.dev.demoraes.abrolhos.domain.exceptions.AccountAlreadyActiveException
import br.dev.demoraes.abrolhos.domain.exceptions.InvalidInviteException
import br.dev.demoraes.abrolhos.domain.exceptions.InvalidTotpCodeException
import br.dev.demoraes.abrolhos.domain.exceptions.UserNotFoundException
import br.dev.demoraes.abrolhos.domain.repository.InviteRepository
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import br.dev.demoraes.abrolhos.infrastructure.monitoring.MetricsService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ulid.ULID
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthServiceTest {
    private val userRepository: UserRepository = mockk()
    private val inviteRepository: InviteRepository = mockk()
    private val totpService: TotpService = mockk()
    private val passwordService: PasswordService = mockk(relaxed = true)
    private val tokenService: TokenService = mockk()
    private val metricsService: MetricsService = mockk(relaxed = true)
    private val auditLogger: br.dev.demoraes.abrolhos.application.audit.AuditLogger =
        mockk(relaxed = true)
    private val rateLimitService: RateLimitService = mockk()

    private val authService =
        AuthService(
            userRepository,
            inviteRepository,
            totpService,
            passwordService,
            tokenService,
            metricsService,
            auditLogger,
            rateLimitService,
        )

    private val testSecret = TotpSecret("JBSWY3DPEHPK3PXP")
    private val testPassword = PlaintextPassword("Test@1234Abc!")
    private val testHash =
        PasswordHash("\$2a\$12\$abcdefghijklmnopqrstuvuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuuu")

    // ---------------------------------------------------------------------------
    // activateAccount tests
    // ---------------------------------------------------------------------------

    @Test
    fun `activateAccount should reject expired invite`() {
        val token = InviteToken("a".repeat(64))
        val expiredInvite =
            Invite(
                id = ULID.nextULID(),
                token = token,
                userId = ULID.nextULID(),
                expiryDate = OffsetDateTime.now().minusDays(1),
                createdAt = OffsetDateTime.now().minusDays(2),
                totpSecret = testSecret
            )

        every { inviteRepository.findByToken(token) } returns expiredInvite
        every { inviteRepository.deleteById(any()) } returns Unit

        assertThrows<InvalidInviteException> {
            authService.activateAccount(token, testPassword, TotpCode("123456"))
        }

        verify { inviteRepository.deleteById(expiredInvite.id) }
    }

    @Test
    fun `activateAccount should reject non-existent invite`() {
        val token = InviteToken("a".repeat(64))
        every { inviteRepository.findByToken(token) } returns null

        assertThrows<InvalidInviteException> {
            authService.activateAccount(token, testPassword, TotpCode("123456"))
        }
    }

    @Test
    fun `activateAccount should reject when user not found`() {
        val token = InviteToken("a".repeat(64))
        val invite =
            Invite(
                id = ULID.nextULID(),
                token = token,
                userId = ULID.nextULID(),
                expiryDate = OffsetDateTime.now().plusDays(1),
                createdAt = OffsetDateTime.now(),
                totpSecret = testSecret
            )

        every { inviteRepository.findByToken(token) } returns invite
        every { userRepository.findById(any()) } returns null

        assertThrows<UserNotFoundException> {
            authService.activateAccount(token, testPassword, TotpCode("123456"))
        }
    }

    @Test
    fun `activateAccount should reject already active user`() {
        val token = InviteToken("a".repeat(64))
        val invite =
            Invite(
                id = ULID.nextULID(),
                token = token,
                userId = ULID.nextULID(),
                expiryDate = OffsetDateTime.now().plusDays(1),
                createdAt = OffsetDateTime.now(),
                totpSecret = testSecret
            )

        val activeUser =
            User(
                id = invite.userId,
                username = Username("testuser"),
                totpSecret = TotpSecret("JBSWY3DPEHPK3PXP"),
                passwordHash = testHash,
                isActive = true,
                role = Role.USER,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )

        every { inviteRepository.findByToken(token) } returns invite
        every { userRepository.findById(any()) } returns activeUser

        assertThrows<AccountAlreadyActiveException> {
            authService.activateAccount(token, testPassword, TotpCode("123456"))
        }
    }

    @Test
    fun `activateAccount should reject invalid TOTP code`() {
        val token = InviteToken("a".repeat(64))
        val totpCode = TotpCode("123456")
        val newSecret = TotpSecret("JBSWY3DPEHPK3PXP")
        val invite =
            Invite(
                id = ULID.nextULID(),
                token = token,
                userId = ULID.nextULID(),
                expiryDate = OffsetDateTime.now().plusDays(1),
                createdAt = OffsetDateTime.now(),
                totpSecret = newSecret
            )

        val inactiveUser =
            User(
                id = invite.userId,
                username = Username("testuser"),
                totpSecret = null,
                passwordHash = null,
                isActive = false,
                role = Role.USER,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )

        every { inviteRepository.findByToken(token) } returns invite
        every { userRepository.findById(any()) } returns inactiveUser
        every { passwordService.validatePassword(testPassword) } returns emptyList()
        every { passwordService.hashPassword(testPassword) } returns testHash
        every { totpService.verifyCode(newSecret, totpCode) } returns false

        assertThrows<InvalidTotpCodeException> {
            authService.activateAccount(token, testPassword, totpCode)
        }
    }

    @Test
    fun `activateAccount should successfully activate user with valid inputs`() {
        val token = InviteToken("a".repeat(64))
        val totpCode = TotpCode("123456")
        val newSecret = TotpSecret("JBSWY3DPEHPK3PXP")
        val invite =
            Invite(
                id = ULID.nextULID(),
                token = token,
                userId = ULID.nextULID(),
                expiryDate = OffsetDateTime.now().plusDays(1),
                createdAt = OffsetDateTime.now(),
                totpSecret = newSecret
            )

        val inactiveUser =
            User(
                id = invite.userId,
                username = Username("testuser"),
                totpSecret = null,
                passwordHash = null,
                isActive = false,
                role = Role.USER,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )

        val expectedToken = "jwt-token-123"

        every { inviteRepository.findByToken(token) } returns invite
        every { userRepository.findById(any()) } returns inactiveUser
        every { passwordService.validatePassword(testPassword) } returns emptyList()
        every { passwordService.hashPassword(testPassword) } returns testHash
        every { totpService.verifyCode(newSecret, totpCode) } returns true
        every { userRepository.save(any()) } answers { firstArg() }
        every { inviteRepository.deleteById(any()) } returns Unit
        every { tokenService.generateToken(any()) } returns expectedToken

        val result = authService.activateAccount(token, testPassword, totpCode)

        assertNotNull(result)
        assertEquals(expectedToken, result)
        verify {
            userRepository.save(
                match {
                    it.isActive && it.totpSecret == newSecret && it.passwordHash == testHash
                }
            )
        }
        verify { inviteRepository.deleteById(invite.id) }
    }

    @Test
    fun `activateAccount should throw IllegalStateException when invite has no persisted secret`() {
        val token = InviteToken("a".repeat(64))
        val totpCode = TotpCode("123456")
        val invite =
            Invite(
                id = ULID.nextULID(),
                token = token,
                userId = ULID.nextULID(),
                expiryDate = OffsetDateTime.now().plusDays(1),
                createdAt = OffsetDateTime.now(),
                totpSecret = null
            )

        val inactiveUser =
            User(
                id = invite.userId,
                username = Username("testuser"),
                totpSecret = null,
                passwordHash = null,
                isActive = false,
                role = Role.USER,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )

        every { inviteRepository.findByToken(token) } returns invite
        every { userRepository.findById(any()) } returns inactiveUser
        every { passwordService.validatePassword(testPassword) } returns emptyList()
        every { passwordService.hashPassword(testPassword) } returns testHash

        val exception =
            assertThrows<IllegalStateException> {
                authService.activateAccount(token, testPassword, totpCode)
            }

        assertEquals(
            "TOTP secret not found in invite. Invite must be validated first.",
            exception.message
        )
    }

    // ---------------------------------------------------------------------------
    // validateInvite tests
    // ---------------------------------------------------------------------------

    @Test
    fun `validateInvite should generate and persist secret for invite without one`() {
        val token = InviteToken("a".repeat(64))
        val invite =
            Invite(
                id = ULID.nextULID(),
                token = token,
                userId = ULID.nextULID(),
                expiryDate = OffsetDateTime.now().plusDays(1),
                createdAt = OffsetDateTime.now(),
                totpSecret = null
            )

        val inactiveUser =
            User(
                id = invite.userId,
                username = Username("testuser"),
                totpSecret = null,
                passwordHash = null,
                isActive = false,
                role = Role.USER,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )

        val newSecret = TotpSecret("JBSWY3DPEHPK3PXP")
        val provisioningUri =
            "otpauth://totp/Abrolhos:testuser?secret=JBSWY3DPEHPK3PXP&issuer=Abrolhos"

        every { inviteRepository.findByToken(token) } returns invite
        every { userRepository.findById(any()) } returns inactiveUser
        every { totpService.generateSecret() } returns newSecret
        every { totpService.validateSecret(newSecret) } returns
            SecretValidation(isValid = true, byteCount = 10, expectedByteCount = 20)
        every { totpService.generateProvisioningUri("testuser", newSecret) } returns provisioningUri
        every { inviteRepository.save(any()) } returns invite.copy(totpSecret = newSecret)

        val result = authService.validateInvite(token)

        assertEquals("testuser", result.username)
        assertEquals(provisioningUri, result.provisioningUri)
        verify { totpService.generateSecret() }
        verify { totpService.validateSecret(newSecret) }
        verify { inviteRepository.save(match { it.totpSecret == newSecret }) }
    }

    @Test
    fun `validateInvite should reuse existing secret without generating a new one`() {
        val token = InviteToken("a".repeat(64))
        val existingSecret = TotpSecret("EXISTINGSECRETVAL")
        val invite =
            Invite(
                id = ULID.nextULID(),
                token = token,
                userId = ULID.nextULID(),
                expiryDate = OffsetDateTime.now().plusDays(1),
                createdAt = OffsetDateTime.now(),
                totpSecret = existingSecret
            )

        val inactiveUser =
            User(
                id = invite.userId,
                username = Username("testuser"),
                totpSecret = null,
                passwordHash = null,
                isActive = false,
                role = Role.USER,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )

        val provisioningUri =
            "otpauth://totp/Abrolhos:testuser?secret=EXISTINGSECRETVAL&issuer=Abrolhos"

        every { inviteRepository.findByToken(token) } returns invite
        every { userRepository.findById(any()) } returns inactiveUser
        every { totpService.generateProvisioningUri("testuser", existingSecret) } returns
            provisioningUri

        val result = authService.validateInvite(token)

        assertEquals(provisioningUri, result.provisioningUri)
        verify(exactly = 0) { totpService.generateSecret() }
        verify(exactly = 0) { inviteRepository.save(any()) }
    }

    @Test
    fun `validateInvite should reject expired invite`() {
        val token = InviteToken("a".repeat(64))
        val expiredInvite =
            Invite(
                id = ULID.nextULID(),
                token = token,
                userId = ULID.nextULID(),
                expiryDate = OffsetDateTime.now().minusDays(1),
                createdAt = OffsetDateTime.now().minusDays(2)
            )

        every { inviteRepository.findByToken(token) } returns expiredInvite

        assertThrows<InvalidInviteException> { authService.validateInvite(token) }
    }

    // ---------------------------------------------------------------------------
    // login tests
    // ---------------------------------------------------------------------------

    @Test
    fun `login should reject non-existent user`() {
        val username = Username("nonexistent")
        val password = testPassword

        every { rateLimitService.tryConsume(any(), any()) } returns
            mockk { every { isAllowed } returns true }
        every { userRepository.findByUsername(username) } returns null

        val exception =
            assertThrows<br.dev.demoraes.abrolhos.domain.exceptions.AuthenticationException> {
                authService.login(username, password, TotpCode("123456"))
            }

        assertEquals("Invalid credentials", exception.message)
    }

    @Test
    fun `login should reject inactive user`() {
        val username = Username("testuser")
        val inactiveUser =
            User(
                id = ULID.nextULID(),
                username = username,
                totpSecret = TotpSecret("JBSWY3DPEHPK3PXP"),
                passwordHash = testHash,
                isActive = false,
                role = Role.USER,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )

        every { rateLimitService.tryConsume(any(), any()) } returns
            mockk { every { isAllowed } returns true }
        every { userRepository.findByUsername(username) } returns inactiveUser

        val exception =
            assertThrows<br.dev.demoraes.abrolhos.domain.exceptions.AuthenticationException> {
                authService.login(username, testPassword, TotpCode("123456"))
            }

        assertEquals("Invalid credentials", exception.message)
    }

    @Test
    fun `login should reject user with null password hash`() {
        val username = Username("testuser")
        val userWithoutPassword =
            User(
                id = ULID.nextULID(),
                username = username,
                totpSecret = TotpSecret("JBSWY3DPEHPK3PXP"),
                passwordHash = null,
                isActive = true,
                role = Role.USER,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )

        every { rateLimitService.tryConsume(any(), any()) } returns
            mockk { every { isAllowed } returns true }
        every { userRepository.findByUsername(username) } returns userWithoutPassword

        val exception =
            assertThrows<br.dev.demoraes.abrolhos.domain.exceptions.AuthenticationException> {
                authService.login(username, testPassword, TotpCode("123456"))
            }

        assertEquals(
            "No password set. Please use the password reset flow to set your password.",
            exception.message
        )
    }

    @Test
    fun `login should reject invalid password`() {
        val username = Username("testuser")
        val totpCode = TotpCode("123456")
        val secret = TotpSecret("JBSWY3DPEHPK3PXP")
        val activeUser =
            User(
                id = ULID.nextULID(),
                username = username,
                totpSecret = secret,
                passwordHash = testHash,
                isActive = true,
                role = Role.USER,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )

        every { rateLimitService.tryConsume(any(), any()) } returns
            mockk { every { isAllowed } returns true }
        every { userRepository.findByUsername(username) } returns activeUser
        every { passwordService.verifyPassword(testPassword, testHash) } returns false

        val exception =
            assertThrows<br.dev.demoraes.abrolhos.domain.exceptions.AuthenticationException> {
                authService.login(username, testPassword, totpCode)
            }

        assertEquals("Invalid credentials", exception.message)
    }

    @Test
    fun `login should reject invalid TOTP code`() {
        val username = Username("testuser")
        val totpCode = TotpCode("123456")
        val secret = TotpSecret("JBSWY3DPEHPK3PXP")
        val activeUser =
            User(
                id = ULID.nextULID(),
                username = username,
                totpSecret = secret,
                passwordHash = testHash,
                isActive = true,
                role = Role.USER,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )

        every { rateLimitService.tryConsume(any(), any()) } returns
            mockk { every { isAllowed } returns true }
        every { userRepository.findByUsername(username) } returns activeUser
        every { passwordService.verifyPassword(testPassword, testHash) } returns true
        every { totpService.verifyCode(secret, totpCode) } returns false

        val exception =
            assertThrows<br.dev.demoraes.abrolhos.domain.exceptions.AuthenticationException> {
                authService.login(username, testPassword, totpCode)
            }

        assertEquals("Invalid credentials", exception.message)
    }

    @Test
    fun `login should successfully authenticate with valid credentials`() {
        val username = Username("testuser")
        val totpCode = TotpCode("123456")
        val secret = TotpSecret("JBSWY3DPEHPK3PXP")
        val activeUser =
            User(
                id = ULID.nextULID(),
                username = username,
                totpSecret = secret,
                passwordHash = testHash,
                isActive = true,
                role = Role.USER,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now()
            )
        val expectedToken = "jwt-token-123"

        every { rateLimitService.tryConsume(any(), any()) } returns
            mockk { every { isAllowed } returns true }
        every { userRepository.findByUsername(username) } returns activeUser
        every { passwordService.verifyPassword(testPassword, testHash) } returns true
        every { totpService.verifyCode(secret, totpCode) } returns true
        every { tokenService.generateToken(activeUser) } returns expectedToken

        val result = authService.login(username, testPassword, totpCode)

        assertNotNull(result)
        assertEquals(expectedToken, result)
        verify { tokenService.generateToken(activeUser) }
    }
}
