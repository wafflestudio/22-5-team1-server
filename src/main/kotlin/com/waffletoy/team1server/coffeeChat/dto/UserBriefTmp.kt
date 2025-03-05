package com.waffletoy.team1server.coffeeChat.dto

import com.waffletoy.team1server.user.UserRole
import com.waffletoy.team1server.user.persistence.UserEntity

data class UserBriefTmp(
    val id: String,
    val name: String,
    val userRole: UserRole,
    val imageLink: String?,
) {
    companion object {
        fun fromEntity(
            entity: UserEntity,
        ): UserBriefTmp =
            UserBriefTmp(
                id = entity.id,
                name = entity.name,
                userRole = entity.userRole,
                imageLink = entity.profileImageLink,
            )
    }
}
