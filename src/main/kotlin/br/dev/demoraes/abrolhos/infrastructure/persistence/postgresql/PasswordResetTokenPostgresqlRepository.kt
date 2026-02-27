package br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql

import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.PasswordResetTokenJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.OffsetDateTime

/**
 * Spring Data JPA repository for [PasswordResetTokenJpaEntity].
 *
 * Provides derived query methods used by [PasswordResetTokenRepositoryImpl].
 */
interface PasswordResetTokenPostgresqlRepository :
    JpaRepository<PasswordResetTokenJpaEntity, String> {

    fun findByToken(token: String): PasswordResetTokenJpaEntity?

    @Modifying
    @Query("DELETE FROM PasswordResetTokenJpaEntity t WHERE t.expiresAt < :now")
    fun deleteByExpiresAtBefore(now: OffsetDateTime)

    @Modifying fun deleteByUserId(userId: String)
}
