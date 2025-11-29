package br.dev.demoraes.abrolhos.infrastructure.security

import br.dev.demoraes.abrolhos.domain.entities.Role
import br.dev.demoraes.abrolhos.domain.entities.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

/**
 * Security adapter that wraps the pure domain User and exposes Spring Security's UserDetails.
 */
class UserPrincipal(
    val user: User,
) : UserDetails {
    override fun getUsername(): String = user.username.value

    override fun getPassword(): String = user.passwordHash.value

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> =
        when (user.role) {
            Role.ADMIN -> mutableListOf(SimpleGrantedAuthority("ROLE_ADMIN"))
            Role.USER -> mutableListOf(SimpleGrantedAuthority("ROLE_USER"))
        }

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = true
}
