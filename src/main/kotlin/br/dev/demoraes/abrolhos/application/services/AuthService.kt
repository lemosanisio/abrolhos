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
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val inviteRepository: InviteRepository,
    private val totpService: TotpService,
    private val tokenService: TokenService,
) {
    @Transactional
    fun activateAccount(inviteToken: InviteToken, totpCode: TotpCode): String {
        // Find invite
        val invite = inviteRepository.findByToken(inviteToken)
            ?: throw InvalidInviteException("Invalid or expired invite token")
        
        // Check expiry
        if (invite.isExpired()) {
            inviteRepository.deleteById(invite.id)
            throw InvalidInviteException("Invite has expired")
        }
        
        // Find user
        val user = userRepository.findById(invite.userId)
            ?: throw UserNotFoundException("User not found")
        
        // Check user is inactive
        if (user.isActive) {
            throw AccountAlreadyActiveException("Account is already active")
        }
        
        // Generate new TOTP secret
        val newSecret = totpService.generateSecret()
        
        // Verify TOTP code against new secret
        if (!totpService.verifyCode(newSecret, totpCode)) {
            throw InvalidTotpCodeException("Invalid TOTP code")
        }
        
        // Activate user
        val activatedUser = user.copy(
            totpSecret = newSecret,
            isActive = true
        )
        userRepository.save(activatedUser)
        
        // Delete invite
        inviteRepository.deleteById(invite.id)
        
        // Generate session token
        return tokenService.generateToken(activatedUser)
    }

    @Transactional(readOnly = true)
    fun login(username: Username, totpCode: TotpCode): String {
        // Find user
        val user = userRepository.findByUsername(username)
            ?: throw AuthenticationException("Invalid credentials")
        
        // Check user is active
        if (!user.isActive) {
            throw AuthenticationException("Invalid credentials")
        }
        
        // Verify TOTP code
        val secret = user.totpSecret 
            ?: throw AuthenticationException("Invalid credentials")
        
        if (!totpService.verifyCode(secret, totpCode)) {
            throw AuthenticationException("Invalid credentials")
        }
        
        // Generate session token
        return tokenService.generateToken(user)
    }
}
