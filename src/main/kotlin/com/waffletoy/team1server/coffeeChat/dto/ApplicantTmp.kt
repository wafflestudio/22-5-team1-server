package com.waffletoy.team1server.coffeeChat.dto

import com.waffletoy.team1server.user.UserRole
import com.waffletoy.team1server.user.persistence.UserEntity
import java.time.LocalDateTime

data class ApplicantTmp(
    val id: String,
    val name: String,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val userRole: UserRole,
    val mail: String?,
    val imageKey: String?,
) {
    companion object {
        fun fromEntity(
            entity: UserEntity,
        ): ApplicantTmp =
            ApplicantTmp(
                id = entity.id,
                name = entity.name,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                userRole = entity.userRole,
                mail = entity.mail,
                imageKey = entity.profileImageLink,
            )
    }
}
