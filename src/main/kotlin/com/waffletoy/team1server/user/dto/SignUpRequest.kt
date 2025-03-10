package com.waffletoy.team1server.user.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.waffletoy.team1server.user.UserRole
import jakarta.validation.Valid
import jakarta.validation.constraints.*

data class SignUpRequest(
    @field:NotNull(message = "authType is required")
    val authType: UserRole,
    @field:Valid
    val info: Info,
) {
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type",
        visible = true,
    )
    @JsonSubTypes(
        JsonSubTypes.Type(value = LocalApplicantInfo::class, name = "APPLICANT"),
        JsonSubTypes.Type(value = LocalCompanyInfo::class, name = "COMPANY"),
    )
    sealed class Info

    @JsonTypeName("APPLICANT")
    data class LocalApplicantInfo(
        @field:NotBlank(message = "name is required")
        val name: String,
        @field:NotBlank(message = "snuMail is required")
        @field:Pattern(
            regexp = "^[a-zA-Z0-9._%+-]+@snu\\.ac\\.kr$",
            message = "snuMail must be a valid SNU email address.",
        )
        val mail: String,
        @field:NotBlank(message = "password is required")
        @field:Pattern(
            regexp = PASSWORD_REGEX,
            message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character.",
        )
        val password: String,
    ) : Info()

    @JsonTypeName("COMPANY")
    data class LocalCompanyInfo(
        @field:NotBlank(message = "secretPassword is required")
        val secretPassword: String,
        @field:NotBlank(message = "name is required")
        val name: String,
        @field:NotBlank(message = "email is required")
        @field:Email(message = "email is required")
        val mail: String,
        @field:NotBlank(message = "password is required")
        @field:Size(
            min = 8,
            max = 20,
            message = "Password must be between 8 and 20 characters.",
        )
        @field:Pattern(
            regexp = PASSWORD_REGEX,
            message = "Password must contain at least 1 uppercase letter, 1 lowercase letter, 1 number, and 1 special character.",
        )
        val password: String,
    ) : Info()

    companion object {
        const val PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!*]).{8,20}$"
    }
}
