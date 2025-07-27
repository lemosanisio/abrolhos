package br.dev.demoraes.abrolhos.infrastructure.persistence.entities

import br.dev.demoraes.abrolhos.domain.authentication.entities.Role
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "users")
data class UserEntity(
    @Id
    val id: String,
    @Column(name = "username", nullable = false, unique = true)
    val username: String,
    @Column(name = "email", nullable = false, unique = true)
    val email: String,
    @Column(name = "password_hash", nullable = false, unique = true)
    val passwordHash: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    val role: Role
    )
