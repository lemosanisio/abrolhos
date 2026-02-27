package br.dev.demoraes.abrolhos.infrastructure.persistence

import br.dev.demoraes.abrolhos.domain.entities.PasswordResetToken
import br.dev.demoraes.abrolhos.domain.entities.PasswordResetTokenEntity
import br.dev.demoraes.abrolhos.domain.repository.PasswordResetTokenRepository
import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.PasswordResetTokenJpaEntity
import br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql.PasswordResetTokenPostgresqlRepository
import java.time.OffsetDateTime
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import ulid.ULID

/**
 * Persistence implementation of [PasswordResetTokenRepository].
 *
 * Translates between the domain [PasswordResetTokenEntity] and the JPA
 * [PasswordResetTokenJpaEntity].
 */
@Repository
class PasswordResetTokenRepositoryImpl(
        private val jpaRepository: PasswordResetTokenPostgresqlRepository,
) : PasswordResetTokenRepository {

    override fun save(token: PasswordResetTokenEntity): PasswordResetTokenEntity {
        val entity =
                PasswordResetTokenJpaEntity(
                                userId = token.userId.toString(),
                                token = token.token.value,
                                expiresAt = token.expiresAt,
                        )
                        .apply { id = token.id.toString() }
        val saved = jpaRepository.save(entity)
        return saved.toDomain()
    }

    override fun findByToken(token: PasswordResetToken): PasswordResetTokenEntity? =
            jpaRepository.findByToken(token.value)?.toDomain()

    override fun deleteById(id: ULID) {
        jpaRepository.deleteById(id.toString())
    }

    @Transactional
    override fun deleteExpiredTokens() {
        jpaRepository.deleteByExpiresAtBefore(OffsetDateTime.now())
    }

    @Transactional
    override fun deleteByUserId(userId: ULID) {
        jpaRepository.deleteByUserId(userId.toString())
    }
}

private fun PasswordResetTokenJpaEntity.toDomain(): PasswordResetTokenEntity {
    val createdAt =
            this.createdAt
                    ?: throw IllegalStateException(
                            "PasswordResetTokenJpaEntity $id is missing createdAt"
                    )

    return PasswordResetTokenEntity(
            id = ULID.parseULID(this.id),
            userId = ULID.parseULID(this.userId),
            token = PasswordResetToken(this.token),
            expiresAt = this.expiresAt,
            createdAt = createdAt,
    )
}
