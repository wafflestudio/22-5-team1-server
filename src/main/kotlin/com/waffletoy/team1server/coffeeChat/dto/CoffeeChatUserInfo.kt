package com.waffletoy.team1server.coffeeChat.dto

import com.fasterxml.jackson.annotation.JsonInclude
import com.waffletoy.team1server.user.persistence.UserEntity

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CoffeeChatUserInfo(
    val name: String,
    val imageKey: String?,
) {
    companion object {
        fun fromEntity(
            entity: UserEntity,
        ): CoffeeChatUserInfo =
            CoffeeChatUserInfo(
                entity.name,
                entity.profileImageLink,
            )
    }
}
