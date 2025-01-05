package com.waffletoy.team1server.user.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<UserEntity, String> {
    fun findBySnuMail(snuMail: String): UserEntity?

    fun findByGoogleId(googleId: String): UserEntity?

    fun findByLocalId(loginID: String): UserEntity?

    fun existsBySnuMail(snuMail: String): Boolean

    fun existsByLocalId(userId: String): Boolean

    fun existsByGoogleId(googleId: String): Boolean
}
