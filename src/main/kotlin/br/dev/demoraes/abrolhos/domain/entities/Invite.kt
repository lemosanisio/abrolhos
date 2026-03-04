package br.dev.demoraes.abrolhos.domain.entities

import ulid.ULID
import java.time.OffsetDateTime

/**
 * Represents an invitation to join the system.
 *
 * Invitations are the mechanism for new users to register/activate their accounts. They are
 * short-lived and link a pre-created (inactive) user to a unique token.
 *
 * @property id Unique identifier (ULID)
 * @property token Secure random token string
 * @property userId The ID of the user to be activated
 * @property expiryDate Expiration timestamp
 * @property totpSecret Temporary storage for TOTP secret during activation flow
 */
data class Invite(
    val id: ULID,
    val token: InviteToken,
    val userId: ULID,
    val expiryDate: OffsetDateTime,
    val createdAt: OffsetDateTime,
    val totpSecret: TotpSecret? = null,
) {
    fun isExpired(): Boolean = OffsetDateTime.now().isAfter(expiryDate)
}
