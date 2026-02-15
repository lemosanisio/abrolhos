package br.dev.demoraes.abrolhos.domain.repository

import br.dev.demoraes.abrolhos.domain.entities.Invite
import br.dev.demoraes.abrolhos.domain.entities.InviteToken
import ulid.ULID

/**
 * Repository interface for Invite entity persistence.
 *
 * Manages the lifecycle of user invitations, including retrieval by token and deletion of
 * expired/used invites.
 */
interface InviteRepository {
    fun findByToken(token: InviteToken): Invite?
    fun save(invite: Invite): Invite
    fun deleteById(id: ULID)
    fun deleteExpiredInvites()
}
