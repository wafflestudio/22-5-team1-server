package com.wafflestudio.internhasha.coffeeChat.dto

import com.wafflestudio.internhasha.coffeeChat.CoffeeChatStatus
import com.wafflestudio.internhasha.coffeeChat.persistence.CoffeeChatEntity
import java.time.LocalDateTime

data class CoffeeChatBrief(
    val id: String,
    // 공고 ID
    val postId: String,
    // 공고 포지션
    val positionType: String,
    // 회사 정보
    val company: CoffeeChatUserInfo,
    // 커피챗 생성, 수정 시간
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    // 커피챗 상태
    val coffeeChatStatus: CoffeeChatStatus,
    val changed: Boolean,
    // 지원자 정보
    val applicant: CoffeeChatUserInfo,
) {
    companion object {
        fun fromEntity(
            entity: CoffeeChatEntity,
        ) = CoffeeChatBrief(
            id = entity.id,
            postId = entity.position.id,
            positionType = entity.position.positionType.displayName(),
            company = entity.position.company.user.let { CoffeeChatUserInfo.fromEntity(it) },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            coffeeChatStatus = entity.coffeeChatStatus,
            changed = entity.changed,
            applicant = entity.applicant.let { CoffeeChatUserInfo.fromEntity(it) },
        )
    }
}
