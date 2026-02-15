package br.dev.demoraes.abrolhos.infrastructure.persistence

import br.dev.demoraes.abrolhos.domain.entities.Invite
import br.dev.demoraes.abrolhos.domain.entities.InviteToken
import br.dev.demoraes.abrolhos.domain.entities.TotpSecret
import br.dev.demoraes.abrolhos.domain.repository.InviteRepository
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.InviteEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql.InviteRepositoryPostgresql
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import ulid.ULID
import java.time.OffsetDateTime

/**
 * Persistence implementation for Invite repository.
 *
 * Bridges the Domain layer (InviteRepository interface) and the Infrastructure layer
 * (JPA/Hibernate). Handles the mapping between Domain entities (Invite) and Persistence entities
 * (InviteEntity).
 */
@Repository
class InviteRepositoryImpl(
    private val inviteRepositoryPostgresql: InviteRepositoryPostgresql,
) : InviteRepository {
    private val logger = LoggerFactory.getLogger(InviteRepositoryImpl::class.java)

    override fun findByToken(token: InviteToken): Invite? {
        logger.info("Searching invite by token")
        return inviteRepositoryPostgresql.findByToken(token.value)?.toDomain()
    }

    override fun save(invite: Invite): Invite {
        logger.info("Saving invite for user ${invite.userId}")
        val entity = invite.toEntity()
        return inviteRepositoryPostgresql.save(entity).toDomain()
    }

    override fun deleteById(id: ULID) {
        logger.info("Deleting invite with id $id")
        inviteRepositoryPostgresql.deleteById(id.toString())
    }

    @Transactional
    override fun deleteExpiredInvites() {
        logger.info("Deleting expired invites")
        inviteRepositoryPostgresql.deleteExpiredInvites(OffsetDateTime.now())
    }
}

internal fun Invite.toEntity() =
    InviteEntity(
        token = this.token.value,
        userId = this.userId.toString(),
        expiryDate = this.expiryDate,
        totpSecret = this.totpSecret?.value,
    )
        .apply {
            id = this@toEntity.id.toString()
            createdAt = this@toEntity.createdAt
            updatedAt = OffsetDateTime.now()
        }

internal fun InviteEntity.toDomain(): Invite {
    val createdAt =
        this.createdAt
            ?: throw IllegalStateException(
                "InviteEntity with id ${this.id} is missing a createdAt timestamp. " +
                    "This should not happen for a persisted entity.",
            )

    return Invite(
        id = ULID.parseULID(this.id),
        token = InviteToken(this.token),
        userId = ULID.parseULID(this.userId),
        expiryDate = this.expiryDate,
        createdAt = createdAt,
        totpSecret = this.totpSecret?.let { TotpSecret(it) },
    )
}
