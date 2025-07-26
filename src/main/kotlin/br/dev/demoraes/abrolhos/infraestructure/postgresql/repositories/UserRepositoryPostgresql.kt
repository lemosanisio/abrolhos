package br.dev.demoraes.abrolhos.infraestructure.postgresql.repositories

import br.dev.demoraes.abrolhos.infraestructure.postgresql.entities.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import ulid.ULID

interface UserRepositoryPostgresql: JpaRepository<UserEntity, ULID> {
    fun findByIdOrNull(id: ULID): UserEntity?
    fun findByUsername(username: String): UserEntity?
}