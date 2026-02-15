package br.dev.demoraes.abrolhos.infrastructure.persistence.entities

import br.dev.demoraes.abrolhos.domain.entities.Role
import br.dev.demoraes.abrolhos.infrastructure.persistence.converters.TotpSecretConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "users")
open class UserEntity(
    @Column(name = "username", nullable = false, unique = true, length = 50)
    open var username: String,
    @Convert(converter = TotpSecretConverter::class)
    @Column(name = "totp_secret", nullable = true, length = 255)
    open var totpSecret: String?,
    @Column(name = "is_active", nullable = false)
    open var isActive: Boolean,
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    open var role: Role,
) : BaseEntity()
