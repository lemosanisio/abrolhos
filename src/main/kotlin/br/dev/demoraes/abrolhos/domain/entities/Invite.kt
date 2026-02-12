package br.dev.demoraes.abrolhos.domain.entities

import ulid.ULID
import java.time.OffsetDateTime

data class Invite(
    val id: ULID,
    val token: InviteToken,
    val userId: ULID,
    val expiryDate: OffsetDateTime,
    val createdAt: OffsetDateTime,
) {
    fun isExpired(): Boolean = OffsetDateTime.now().isAfter(expiryDate)
}
