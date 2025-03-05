package com.waffletoy.team1server.coffeeChat.dto

import com.waffletoy.team1server.coffeeChat.CoffeeChatStatus
import com.waffletoy.team1server.coffeeChat.persistence.CoffeeChatEntity
import java.time.LocalDateTime

data class CoffeeChatCompany(
    val id: String,
    // 공고 ID
    val postId: String,
    // 공고 제목
    val title: String,
    // 지원자 정보
    val applicant: ApplicantTmp,
    // 회사명, 회사 이미지 url(imageLink)
    val company: UserBriefTmp,
    // 내용
    val content: String,
    // 커피챗 생성, 수정 시간
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    // 커피챗 상태
    val coffeeChatStatus: CoffeeChatStatus,
    val changed: Boolean,
) {
    companion object {
        fun fromEntity(
            entity: CoffeeChatEntity,
        ) = CoffeeChatCompany(
            id = entity.id,
            postId = entity.position.id,
            title = entity.position.title,
            applicant = entity.applicant.let { ApplicantTmp.fromEntity(it) },
            company = entity.position.company.curator.let { UserBriefTmp.fromEntity(it) },
            content = entity.content,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            coffeeChatStatus = entity.coffeeChatStatus,
            changed = entity.changed,
        )
    }
}
