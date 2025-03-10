package com.waffletoy.team1server.coffeeChat.dto

import com.waffletoy.team1server.coffeeChat.CoffeeChatStatus
import com.waffletoy.team1server.coffeeChat.persistence.CoffeeChatEntity
import java.time.LocalDateTime

data class CoffeeChatBrief(
    val id: String,
    // 공고 ID
    val postId: String,
    // 공고 제목
    val title: String,
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
            title = entity.position.title,
            company = entity.position.company.company.let { CoffeeChatUserInfo.fromEntity(it) },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            coffeeChatStatus = entity.coffeeChatStatus,
            changed = entity.changed,
            applicant = entity.applicant.let { CoffeeChatUserInfo.fromEntity(it) },
        )
    }
}
