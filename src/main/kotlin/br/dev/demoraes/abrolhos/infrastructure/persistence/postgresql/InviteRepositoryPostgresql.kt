package br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql

import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.InviteEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

interface InviteRepositoryPostgresql : JpaRepository<InviteEntity, String> {
    @Query("SELECT i FROM InviteEntity i WHERE i.token = :token")
    fun findByToken(
        @Param("token") token: String,
    ): InviteEntity?

    @Modifying
    @Query("DELETE FROM InviteEntity i WHERE i.expiryDate < :now")
    fun deleteExpiredInvites(
        @Param("now") now: OffsetDateTime,
    )
}
