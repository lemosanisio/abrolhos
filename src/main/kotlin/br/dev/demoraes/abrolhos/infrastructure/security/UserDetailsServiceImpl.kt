package br.dev.demoraes.abrolhos.infrastructure.security

import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val domainUser =
            userRepository.findByUsername(Username(username))
                ?: throw UsernameNotFoundException("User '$username' not found")
        return UserPrincipal(domainUser)
    }
}
