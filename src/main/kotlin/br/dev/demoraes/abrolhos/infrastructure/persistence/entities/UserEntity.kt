package br.dev.demoraes.abrolhos.infrastructure.persistence.entities

import br.dev.demoraes.abrolhos.domain.entities.Role
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "users")
open class UserEntity(
    @Column(name = "username", nullable = false, unique = true, length = 50)
    open var username: String,
    @Column(name = "email", nullable = false, unique = true, length = 255)
    open var email: String,
    @Column(name = "password_hash", nullable = false, length = 255)
    open var passwordHash: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    open var role: Role
) : BaseEntity()
