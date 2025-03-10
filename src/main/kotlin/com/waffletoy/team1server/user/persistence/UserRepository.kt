package com.waffletoy.team1server.user.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, String> {
    fun findByMail(snuMail: String): UserEntity?

    fun existsByMail(snuMail: String): Boolean

    fun deleteUserEntityById(id: String)
}
