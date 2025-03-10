package com.waffletoy.team1server.post.dto

import com.waffletoy.team1server.post.Category
import com.waffletoy.team1server.post.Series
import com.waffletoy.team1server.post.persistence.PositionEntity
import java.time.LocalDateTime

data class Post(
    val id: String,
    // 작성자
    val author: AuthorBrief,
    // 회사 정보
    val companyName: String,
    val explanation: String,
    val email: String,
    val slogan: String,
    val investAmount: Int,
    val investCompany: String,
    val series: Series,
    val irDeckLink: String?,
    val landingPageLink: String,
    val imageLink: String,
    val links: List<Link>,
    val tags: List<String>,
    // 직군 정보
    val title: String,
    val employmentEndDate: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isActive: Boolean,
    val category: Category,
    val detail: String,
    val headcount: Int,
    // 북마크 여부
    val isBookmarked: Boolean = false,
) {
    companion object {
        fun fromEntity(
            entity: PositionEntity,
            isBookmarked: Boolean = false,
            isLoggedIn: Boolean,
        ): Post =
            Post(
                id = entity.id,
                author =
                    AuthorBrief(
                        id = entity.company.company.id,
                        name = entity.company.company.name,
                        profileImageLink = entity.company.company.profileImageLink,
                    ),
                companyName = entity.company.companyName,
                explanation = entity.company.explanation ?: "",
                email = entity.company.email,
                slogan = entity.company.slogan ?: "",
                investAmount = entity.company.investAmount,
                investCompany = entity.company.investCompany ?: "",
                series = entity.company.series,
                irDeckLink = if (isLoggedIn) entity.company.irDeckLink ?: "" else null,
                landingPageLink = entity.company.landingPageLink ?: "",
                imageLink = entity.company.imageLink ?: "",
                links = entity.company.links.map { Link.fromVo(it) },
                tags = entity.company.tags.map { it.tag },
                // roles 정보
                title = entity.title,
                // 생성 시간은 Roles 생성 기준
                employmentEndDate = entity.employmentEndDate,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                isActive = entity.isActive,
                category = entity.category,
                detail = entity.detail ?: "",
                headcount = entity.headcount,
                isBookmarked = isBookmarked,
            )
    }
}
