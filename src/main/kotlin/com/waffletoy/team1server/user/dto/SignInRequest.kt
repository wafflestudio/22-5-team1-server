package com.waffletoy.team1server.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class SignInRequest(
    @field:NotBlank(message = "mail is required")
    @field:Email(message = "Invalid email format.")
    val mail: String,
    @field:NotBlank(message = "password is required")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!*]).{8,20}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character.",
    )
    val password: String,
)
