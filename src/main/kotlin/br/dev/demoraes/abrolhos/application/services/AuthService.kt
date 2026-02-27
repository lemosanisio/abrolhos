package br.dev.demoraes.abrolhos.application.services

import br.dev.demoraes.abrolhos.domain.entities.InviteToken
import br.dev.demoraes.abrolhos.domain.entities.TotpCode
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.exceptions.AccountAlreadyActiveException
import br.dev.demoraes.abrolhos.domain.exceptions.AuthenticationException
import br.dev.demoraes.abrolhos.domain.exceptions.InvalidInviteException
import br.dev.demoraes.abrolhos.domain.exceptions.InvalidTotpCodeException
import br.dev.demoraes.abrolhos.domain.exceptions.UserNotFoundException
import br.dev.demoraes.abrolhos.domain.repository.InviteRepository
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import br.dev.demoraes.abrolhos.infrastructure.monitoring.MetricsService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service responsible for the authentication flow.
 *
 * This service orchestrates the user onboarding and login processes:
 * 1. `validateInvite`: Checks if an invite token is valid and generates/retrieves a TOTP secret.
 * 2. `activateAccount`: Verifies the TOTP code provided by the user and activates the account.
 * 3. `login`: Authenticates an active user using their username and TOTP code.
 *
 * It bridges the gap between the storage (Repositories) and the security mechanisms (TotpService,
 * TokenService).
 */
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val inviteRepository: InviteRepository,
    private val totpService: TotpService,
    private val tokenService: TokenService,
    private val metricsService: MetricsService,
) {
    data class InvitationDetails(val username: String, val provisioningUri: String)

    @Suppress("ThrowsCount")
    @Transactional
    fun validateInvite(inviteToken: InviteToken): InvitationDetails {
        // Find invite
        val invite =
            inviteRepository.findByToken(inviteToken)
                ?: throw InvalidInviteException("Invalid or expired invite token")

        // Check expiry
        if (invite.isExpired()) {
            throw InvalidInviteException("Invite has expired")
        }

        // Find user
        val user =
            userRepository.findById(invite.userId)
                ?: throw UserNotFoundException("User not found")

        // Check user is inactive
        if (user.isActive) {
            throw AccountAlreadyActiveException("Account is already active")
        }

        // Reuse existing secret or generate a new one and persist it
        val secret =
            invite.totpSecret
                ?: totpService.generateSecret().also { newSecret ->
                    // Log secret generation (first 8 chars for security)
                    println(
                        "Generated new TOTP secret for invite: ${newSecret.value.take(TotpService.SECRET_PREFIX_LENGTH)}..."
                    )

                    // Validate secret before persisting
                    val validation = totpService.validateSecret(newSecret)
                    if (!validation.isValid) {
                        error("Generated TOTP secret is invalid: ${validation.error}")
                    }

                    // Log validation success
                    println(
                        "Secret validation passed: ${validation.byteCount} bytes (expected: ${validation.expectedByteCount})"
                    )

                    inviteRepository.save(invite.copy(totpSecret = newSecret))
                }
        val uri = totpService.generateProvisioningUri(user.username.value, secret)

        return InvitationDetails(username = user.username.value, provisioningUri = uri)
    }

    @Suppress("ThrowsCount")
    @Transactional
    fun activateAccount(inviteToken: InviteToken, totpCode: TotpCode): String {
        // Find invite
        val invite =
            inviteRepository.findByToken(inviteToken)
                ?: throw InvalidInviteException("Invalid or expired invite token")

        // Check expiry
        if (invite.isExpired()) {
            inviteRepository.deleteById(invite.id)
            throw InvalidInviteException("Invite has expired")
        }

        // Find user
        val user =
            userRepository.findById(invite.userId)
                ?: throw UserNotFoundException("User not found")

        // Check user is inactive
        if (user.isActive) {
            throw AccountAlreadyActiveException("Account is already active")
        }

        // Use persisted secret from invite
        val secret =
            invite.totpSecret
                ?: error("TOTP secret not found in invite. Invite must be validated first.")

        // Log the secret being used for verification (first 8 chars for security)
        println(
            "Activating account with persisted secret: ${secret.value.take(TotpService.SECRET_PREFIX_LENGTH)}..."
        )

        // Verify TOTP code against the persisted secret
        if (!totpService.verifyCode(secret, totpCode)) {
            throw InvalidTotpCodeException("Invalid TOTP code")
        }

        // Activate user
        val activatedUser = user.copy(totpSecret = secret, isActive = true)

        // Activate user

        userRepository.save(activatedUser)

        // Delete invite
        inviteRepository.deleteById(invite.id)

        // Generate session token
        return tokenService.generateToken(activatedUser)
    }

    @Suppress("ThrowsCount")
    @Transactional(readOnly = true)
    fun login(username: Username, totpCode: TotpCode): String {
        metricsService.recordLoginAttempt()

        // Find user
        val user =
            userRepository.findByUsername(username)
                ?: run {
                    metricsService.recordLoginFailure()
                    throw AuthenticationException("Invalid credentials")
                }

        // Check user is active
        if (!user.isActive) {
            metricsService.recordLoginFailure()
            throw AuthenticationException("Invalid credentials")
        }

        // Verify TOTP code
        val secret =
            user.totpSecret
                ?: run {
                    metricsService.recordLoginFailure()
                    throw AuthenticationException("Invalid credentials")
                }

        if (!totpService.verifyCode(secret, totpCode)) {
            metricsService.recordLoginFailure()
            throw AuthenticationException("Invalid credentials")
        }

        // Generate session token
        metricsService.recordLoginSuccess()
        return tokenService.generateToken(user)
    }
}
