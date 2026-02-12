package br.dev.demoraes.abrolhos.infrastructure.persistence.postgresql

import br.dev.demoraes.abrolhos.infrastructure.persistence.entities.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface UserRepositoryPostgresql : JpaRepository<UserEntity, String> {
    @Query("SELECT u FROM UserEntity u WHERE u.id = :id")
    fun findByIdOrNull(
        @Param("id") id: String,
    ): UserEntity?

    @Query("SELECT u FROM UserEntity u WHERE u.username = :username")
    fun findByUsername(
        @Param("username") username: String,
    ): UserEntity?

    fun existsByUsername(username: String): Boolean
}
