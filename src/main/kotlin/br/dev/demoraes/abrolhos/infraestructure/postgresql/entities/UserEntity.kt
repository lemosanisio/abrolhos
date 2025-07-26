package br.dev.demoraes.abrolhos.infraestructure.postgresql.entities

import br.dev.demoraes.abrolhos.domain.authentication.entities.Role
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import ulid.ULID

@Entity
@Table(name = "users")
data class UserEntity(
    @Id
    val id: ULID,
    @Column(nullable = false, unique = true)
    val username: String,
    @Column(nullable = false, unique = true)
    val email: String,
    @Column("password_hash", nullable = false, unique = true)
    val passwordHash: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: Role
    )
