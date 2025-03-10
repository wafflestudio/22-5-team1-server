package com.waffletoy.team1server.user.dto

data class UserWithAccessToken(
    val user: UserBrief,
    val token: String,
)
