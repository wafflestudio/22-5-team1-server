package com.waffletoy.team1server.company.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.URL

data class CreateCompanyRequest(
    @field:Min(value = 1800, message = "Established year must be after 1800.")
    val companyEstablishedYear: Int,
    @field:NotBlank(message = "Domain is required.")
    val domain: String,
    @field:Min(value = 1, message = "Headcount must be at least 1.")
    val headcount: Int,
    @field:NotBlank(message = "Location is required.")
    val location: String,
    @field:NotBlank(message = "Slogan is required.")
    val slogan: String,
    @field:NotBlank(message = "Detail description is required.")
    val detail: String,
    @field:NotBlank(message = "Profile image key is required.")
    val profileImageKey: String,
    @field:URL(message = "Company PDF link must be a valid URL.")
    val companyInfoPDFLink: String? = null,
    @field:URL(message = "Landing page link must be a valid URL.")
    val landingPageLink: String,
    @field:NotBlank(message = "VC name is required.")
    val vcName: String,
    @field:NotBlank(message = "VC recommendation is required.")
    val vcRec: String,
    @field:Valid
    val links: List<Link> = emptyList(),
    @field:Valid
    val tags: List<Tag> = emptyList(),
)

typealias UpdateCompanyRequest = CreateCompanyRequest
