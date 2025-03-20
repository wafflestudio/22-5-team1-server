package com.waffletoy.team1server.coffeeChat.dto

import com.waffletoy.team1server.coffeeChat.CoffeeChatStatus
import com.waffletoy.team1server.coffeeChat.persistence.CoffeeChatEntity
import java.time.LocalDateTime

sealed class CoffeeChatDetail

data class CoffeeChatApplicant(
    val id: String,
    // 공고 ID
    val postId: String,
    // 공고 제목
    val title: String,
    // 회사명, 회사 이미지 url
    val user: CoffeeChatUserInfo,
    // 커피챗 생성, 수정 시간
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    // 커피챗 상태
    val coffeeChatStatus: CoffeeChatStatus,
    val changed: Boolean,
    // 내용
    val content: String,
) : CoffeeChatDetail() {
    companion object {
        fun fromEntity(
            entity: CoffeeChatEntity,
        ) = CoffeeChatApplicant(
            id = entity.id,
            postId = entity.position.id,
            title = entity.position.positionTitle,
            user = entity.position.company.user.let { CoffeeChatUserInfo.fromEntity(it) },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            coffeeChatStatus = entity.coffeeChatStatus,
            changed = entity.changed,
            content = entity.content,
        )
    }
}

data class CoffeeChatCompany(
    val id: String,
    // 공고 ID
    val postId: String,
    // 공고 제목
    val title: String,
    // 회사명, 회사 이미지 url(imageLink)
    val user: CoffeeChatUserInfo,
    // 커피챗 생성, 수정 시간
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    // 커피챗 상태
    val coffeeChatStatus: CoffeeChatStatus,
    val changed: Boolean,
    // 내용
    val content: String,
    // 지원자 정보
    val applicant: ApplicantTmp,
) : CoffeeChatDetail() {
    companion object {
        fun fromEntity(
            entity: CoffeeChatEntity,
        ) = CoffeeChatCompany(
            id = entity.id,
            postId = entity.position.id,
            title = entity.position.positionTitle,
            user = entity.position.company.user.let { CoffeeChatUserInfo.fromEntity(it) },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            coffeeChatStatus = entity.coffeeChatStatus,
            changed = entity.changed,
            content = entity.content,
            applicant = entity.applicant.let { ApplicantTmp.fromEntity(it) },
        )
    }
}
