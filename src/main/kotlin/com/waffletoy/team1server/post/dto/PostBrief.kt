package com.waffletoy.team1server.post.dto

import com.waffletoy.team1server.post.Category
import com.waffletoy.team1server.post.Series
import java.time.LocalDateTime

data class PostBrief(
    val id: String,
    val author: AuthorBrief,
    val companyName: String,
    val slogan: String,
    val email: String,
    val investAmount: Int,
    val investCompany: String,
    val series: Series,
    val imageLink: String,
    val title: String,
    val employmentEndDate: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isActive: Boolean,
    val category: Category,
    val headcount: Int,
    val isBookmarked: Boolean,
) {
    companion object {
        fun fromPost(post: Post): PostBrief =
            PostBrief(
                id = post.id,
                author = post.author,
                companyName = post.companyName,
                slogan = post.slogan,
                email = post.email,
                investAmount = post.investAmount,
                investCompany = post.investCompany,
                series = post.series,
                imageLink = post.imageLink,
                title = post.title,
                employmentEndDate = post.employmentEndDate,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt,
                isActive = post.isActive,
                category = post.category,
                headcount = post.headcount,
                isBookmarked = post.isBookmarked,
            )
    }
}
