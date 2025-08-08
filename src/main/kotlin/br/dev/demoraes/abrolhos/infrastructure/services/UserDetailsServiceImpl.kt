package br.dev.demoraes.abrolhos.infrastructure.services

import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val usernameValueObject = Username(username)

        return userRepository.findByUsername(usernameValueObject)
            ?: throw UsernameNotFoundException("User with username '$username' not found.")
    }
}
