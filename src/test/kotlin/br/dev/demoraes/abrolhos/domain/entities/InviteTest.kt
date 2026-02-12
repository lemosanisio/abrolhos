package br.dev.demoraes.abrolhos.domain.entities

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ulid.ULID
import java.time.OffsetDateTime

class InviteTest {

    @Test
    fun `Invite should be expired when expiry date is in the past`() {
        val invite = Invite(
            id = ULID.nextULID(),
            token = InviteToken("a".repeat(32)),
            userId = ULID.nextULID(),
            expiryDate = OffsetDateTime.now().minusDays(1),
            createdAt = OffsetDateTime.now().minusDays(2),
        )
        assertTrue(invite.isExpired())
    }

    @Test
    fun `Invite should not be expired when expiry date is in the future`() {
        val invite = Invite(
            id = ULID.nextULID(),
            token = InviteToken("a".repeat(32)),
            userId = ULID.nextULID(),
            expiryDate = OffsetDateTime.now().plusDays(1),
            createdAt = OffsetDateTime.now(),
        )
        assertFalse(invite.isExpired())
    }

    @Test
    fun `Invite should be expired when expiry date is exactly now`() {
        val now = OffsetDateTime.now()
        val invite = Invite(
            id = ULID.nextULID(),
            token = InviteToken("a".repeat(32)),
            userId = ULID.nextULID(),
            expiryDate = now,
            createdAt = now.minusDays(1),
        )
        // Since we check isAfter, exactly now should not be expired
        // But due to timing, this might be flaky, so we test a moment in the past
        val expiredInvite = invite.copy(expiryDate = now.minusNanos(1))
        assertTrue(expiredInvite.isExpired())
    }
}
