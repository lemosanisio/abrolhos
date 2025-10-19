package br.dev.demoraes.abrolhos.infrastructure.services

import br.dev.demoraes.abrolhos.domain.entities.Email
import br.dev.demoraes.abrolhos.domain.entities.PasswordHash
import br.dev.demoraes.abrolhos.domain.entities.Role
import br.dev.demoraes.abrolhos.domain.entities.User
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.core.userdetails.UsernameNotFoundException
import ulid.ULID
import java.time.OffsetDateTime

class UserDetailsServiceImplTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userDetailsService: UserDetailsServiceImpl

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        userDetailsService = UserDetailsServiceImpl(userRepository)
    }

    @Test
    fun `should load user by username when user exists`() {
        val username = Username("testuser")
        val user = User(
            id = ULID.parseULID(ULID.randomULID()),
            username = username,
            email = Email("test@test.com"),
            passwordHash = PasswordHash("hashed_password"),
            role = Role.USER,
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now()
        )

        every { userRepository.findByUsername(username) } returns user

        val userDetails = userDetailsService.loadUserByUsername("testuser")

        assertEquals(user.username.value, userDetails.username)
    }

    @Test
    fun `should throw UsernameNotFoundException when user does not exist`() {
        val username = "nonexistentuser"
        every { userRepository.findByUsername(Username(username)) } returns null

        val exception = assertThrows<UsernameNotFoundException> {
            userDetailsService.loadUserByUsername(username)
        }

        assertEquals("User with username '$username' not found.", exception.message)
    }
}
