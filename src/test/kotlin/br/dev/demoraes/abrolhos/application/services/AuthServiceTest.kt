package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.domain.entities.Invite
import br.dev.demoraes.abrolhos.domain.entities.InviteToken
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
    private val tokenService: TokenService = mockk()
    private val authService = AuthService(userRepository, inviteRepository, totpService, tokenService)

    @Test
    fun `activateAccount should reject expired invite`() {
        val token = InviteToken("a".repeat(64))
        val expiredInvite = Invite(
            id = ULID.nextULID(),
            token = token,
            userId = ULID.nextULID(),
            expiryDate = OffsetDateTime.now().minusDays(1),
            createdAt = OffsetDateTime.now().minusDays(2)
        )

        every { inviteRepository.findByToken(token) } returns expiredInvite
        every { inviteRepository.deleteById(any()) } returns Unit

        assertThrows<InvalidInviteException> {
            authService.activateAccount(token, TotpCode("123456"))
        }

        verify { inviteRepository.deleteById(expiredInvite.id) }
    }

    @Test
    fun `activateAccount should reject non-existent invite`() {
        val token = InviteToken("a".repeat(64))
        every { inviteRepository.findByToken(token) } returns null

        assertThrows<InvalidInviteException> {
            authService.activateAccount(token, TotpCode("123456"))
        }
    }

    @Test
    fun `activateAccount should reject when user not found`() {
        val token = InviteToken("a".repeat(64))
        val invite = Invite(
            id = ULID.nextULID(),
            token = token,
            userId = ULID.nextULID(),
            expiryDate = OffsetDateTime.now().plusDays(1),
            createdAt = OffsetDateTime.now()
        )

        every { inviteRepository.findByToken(token) } returns invite
        every { userRepository.findById(any()) } returns null

        assertThrows<UserNotFoundException> {
            authService.activateAccount(token, TotpCode("123456"))
        }
    }

    @Test
    fun `activateAccount should reject already active user`() {
        val token = InviteToken("a".repeat(64))
        val invite = Invite(
            id = ULID.nextULID(),
            token = token,
            userId = ULID.nextULID(),
            expiryDate = OffsetDateTime.now().plusDays(1),
            createdAt = OffsetDateTime.now()
        )

        val activeUser = User(
            id = invite.userId,
            username = Username("testuser"),
            totpSecret = TotpSecret("JBSWY3DPEHPK3PXP"),
            isActive = true,
            role = Role.USER,
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )

        every { inviteRepository.findByToken(token) } returns invite
        every { userRepository.findById(any()) } returns activeUser

        assertThrows<AccountAlreadyActiveException> {
            authService.activateAccount(token, TotpCode("123456"))
        }
    }

    @Test
    fun `activateAccount should reject invalid TOTP code`() {
        val token = InviteToken("a".repeat(64))
        val totpCode = TotpCode("123456")
        val invite = Invite(
            id = ULID.nextULID(),
            token = token,
            userId = ULID.nextULID(),
            expiryDate = OffsetDateTime.now().plusDays(1),
            createdAt = OffsetDateTime.now()
        )

        val inactiveUser = User(
            id = invite.userId,
            username = Username("testuser"),
            totpSecret = null,
            isActive = false,
            role = Role.USER,
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )

        val newSecret = TotpSecret("JBSWY3DPEHPK3PXP")

        every { inviteRepository.findByToken(token) } returns invite
        every { userRepository.findById(any()) } returns inactiveUser
        every { totpService.generateSecret() } returns newSecret
        every { totpService.verifyCode(newSecret, totpCode) } returns false

        assertThrows<InvalidTotpCodeException> {
            authService.activateAccount(token, totpCode)
        }
    }

    @Test
    fun `activateAccount should successfully activate user with valid inputs`() {
        val token = InviteToken("a".repeat(64))
        val totpCode = TotpCode("123456")
        val invite = Invite(
            id = ULID.nextULID(),
            token = token,
            userId = ULID.nextULID(),
            expiryDate = OffsetDateTime.now().plusDays(1),
            createdAt = OffsetDateTime.now()
        )

        val inactiveUser = User(
            id = invite.userId,
            username = Username("testuser"),
            totpSecret = null,
            isActive = false,
            role = Role.USER,
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )

        val newSecret = TotpSecret("JBSWY3DPEHPK3PXP")
        val activatedUser = inactiveUser.copy(totpSecret = newSecret, isActive = true)
        val expectedToken = "jwt-token-123"

        every { inviteRepository.findByToken(token) } returns invite
        every { userRepository.findById(any()) } returns inactiveUser
        every { totpService.generateSecret() } returns newSecret
        every { totpService.verifyCode(newSecret, totpCode) } returns true
        every { userRepository.save(any()) } returns activatedUser
        every { inviteRepository.deleteById(any()) } returns Unit
        every { tokenService.generateToken(any()) } returns expectedToken

        val result = authService.activateAccount(token, totpCode)

        assertNotNull(result)
        assertEquals(expectedToken, result)
        verify { userRepository.save(match { it.isActive && it.totpSecret == newSecret }) }
        verify { inviteRepository.deleteById(invite.id) }
        verify { tokenService.generateToken(activatedUser) }
    }

    @Test
    fun `login should reject non-existent user`() {
        val username = Username("nonexistent")
        val totpCode = TotpCode("123456")

        every { userRepository.findByUsername(username) } returns null

        val exception = assertThrows<br.dev.demoraes.abrolhos.domain.exceptions.AuthenticationException> {
            authService.login(username, totpCode)
        }

        assertEquals("Invalid credentials", exception.message)
    }

    @Test
    fun `login should reject inactive user`() {
        val username = Username("testuser")
        val totpCode = TotpCode("123456")
        val inactiveUser = User(
            id = ULID.nextULID(),
            username = username,
            totpSecret = TotpSecret("JBSWY3DPEHPK3PXP"),
            isActive = false,
            role = Role.USER,
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )

        every { userRepository.findByUsername(username) } returns inactiveUser

        val exception = assertThrows<br.dev.demoraes.abrolhos.domain.exceptions.AuthenticationException> {
            authService.login(username, totpCode)
        }

        assertEquals("Invalid credentials", exception.message)
    }

    @Test
    fun `login should reject user with null TOTP secret`() {
        val username = Username("testuser")
        val totpCode = TotpCode("123456")
        val userWithoutSecret = User(
            id = ULID.nextULID(),
            username = username,
            totpSecret = null,
            isActive = true,
            role = Role.USER,
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )

        every { userRepository.findByUsername(username) } returns userWithoutSecret

        val exception = assertThrows<br.dev.demoraes.abrolhos.domain.exceptions.AuthenticationException> {
            authService.login(username, totpCode)
        }

        assertEquals("Invalid credentials", exception.message)
    }

    @Test
    fun `login should reject invalid TOTP code`() {
        val username = Username("testuser")
        val totpCode = TotpCode("123456")
        val secret = TotpSecret("JBSWY3DPEHPK3PXP")
        val activeUser = User(
            id = ULID.nextULID(),
            username = username,
            totpSecret = secret,
            isActive = true,
            role = Role.USER,
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )

        every { userRepository.findByUsername(username) } returns activeUser
        every { totpService.verifyCode(secret, totpCode) } returns false

        val exception = assertThrows<br.dev.demoraes.abrolhos.domain.exceptions.AuthenticationException> {
            authService.login(username, totpCode)
        }

        assertEquals("Invalid credentials", exception.message)
    }

    @Test
    fun `login should successfully authenticate with valid credentials`() {
        val username = Username("testuser")
        val totpCode = TotpCode("123456")
        val secret = TotpSecret("JBSWY3DPEHPK3PXP")
        val activeUser = User(
            id = ULID.nextULID(),
            username = username,
            totpSecret = secret,
            isActive = true,
            role = Role.USER,
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )
        val expectedToken = "jwt-token-123"

        every { userRepository.findByUsername(username) } returns activeUser
        every { totpService.verifyCode(secret, totpCode) } returns true
        every { tokenService.generateToken(activeUser) } returns expectedToken

        val result = authService.login(username, totpCode)

        assertNotNull(result)
        assertEquals(expectedToken, result)
        verify { tokenService.generateToken(activeUser) }
    }
}
