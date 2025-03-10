package com.waffletoy.team1server.user.dto

import com.waffletoy.team1server.user.UserRole
import com.waffletoy.team1server.user.persistence.UserEntity

data class UserBrief(
    val id: String,
    val userRole: UserRole,
) {
    companion object {
        fun fromEntity(
            entity: UserEntity,
        ): UserBrief =
            UserBrief(
                id = entity.id,
                userRole = entity.userRole,
            )
    }
}
