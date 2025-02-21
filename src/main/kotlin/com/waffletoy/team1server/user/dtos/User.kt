package com.waffletoy.team1server.user.dtos

import com.waffletoy.team1server.user.UserRole
import com.waffletoy.team1server.user.persistence.UserEntity
import java.time.LocalDateTime

data class User(
    val id: String,
    val name: String,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val userRole: UserRole,
    val snuMail: String?,
    val phoneNumber: String?,
    val profileImageLink: String?,
    val isMerged: Boolean,
) {
    companion object {
        fun fromEntity(
            entity: UserEntity,
            isMerged: Boolean = false,
//            includeResumes: Boolean = false,
        ): User =
            User(
                id = entity.id,
                name = entity.name,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                userRole = entity.userRole,
                snuMail = entity.snuMail,
                phoneNumber = entity.phoneNumber,
                profileImageLink = entity.profileImageLink,
                isMerged = isMerged,
            )
    }
}
