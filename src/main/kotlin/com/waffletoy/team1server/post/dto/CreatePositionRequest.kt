package com.waffletoy.team1server.post.dto

import com.waffletoy.team1server.post.Category
import jakarta.validation.constraints.*
import java.time.LocalDateTime

data class CreatePositionRequest(
    @field:NotBlank(message = "Position title is required.")
    val positionTitle: String,
    @field:NotNull(message = "Position type (category) is required.")
    val positionType: Category,
    @field:NotBlank(message = "Position detail is required.")
    val detail: String,
    @field:Max(value = 9999, message = "Headcount must not exceed 9999.")
    @field:Min(value = 1, message = "Headcount must be at least 1.")
    val headCount: Int,
    val salary: Int? = null,
    val employmentEndDate: LocalDateTime? = null,
    val isActive: Boolean = true,
    @field:NotBlank(message = "Company ID is required.")
    val companyId: String,
)

typealias UpdatePositionRequest = CreatePositionRequest
