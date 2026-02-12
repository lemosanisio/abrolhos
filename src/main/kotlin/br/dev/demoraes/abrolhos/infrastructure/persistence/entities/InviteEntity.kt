package br.dev.demoraes.abrolhos.infrastructure.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "invites")
open class InviteEntity(
    @Column(name = "token", nullable = false, unique = true, length = 64)
    open var token: String,
    @Column(name = "user_id", nullable = false, length = 26)
    open var userId: String,
    @Column(name = "expiry_date", nullable = false)
    open var expiryDate: OffsetDateTime,
) : BaseEntity()
