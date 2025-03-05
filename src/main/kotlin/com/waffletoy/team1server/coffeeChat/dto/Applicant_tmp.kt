package com.waffletoy.team1server.coffeeChat.dto

import com.waffletoy.team1server.user.UserRole
import com.waffletoy.team1server.user.persistence.UserEntity
import java.time.LocalDateTime

data class Applicant_tmp(
    val id: String,
    val name: String,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val userRole: UserRole,
    val snuMail: String?,
    val phoneNumber: String?,
    val imageLink: String?,
) {
    companion object {
        fun fromEntity(
            entity: UserEntity,
        ): Applicant_tmp =
            Applicant_tmp(
                id = entity.id,
                name = entity.name,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                userRole = entity.userRole,
                snuMail = entity.snuMail,
                phoneNumber = entity.phoneNumber,
                imageLink = entity.profileImageLink,
            )
    }
}
