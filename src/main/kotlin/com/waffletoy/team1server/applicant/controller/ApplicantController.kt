package com.waffletoy.team1server.applicant.controller

import com.waffletoy.team1server.applicant.dto.ApplicantResponse
import com.waffletoy.team1server.applicant.dto.PutApplicantRequest
import com.waffletoy.team1server.applicant.persistence.ApplicantEntity
import com.waffletoy.team1server.applicant.service.ApplicantService
import com.waffletoy.team1server.auth.AuthUser
import com.waffletoy.team1server.auth.dto.User
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/applicant")
class ApplicantController(
    private val applicantService: ApplicantService,
) {
    @GetMapping("/me")
    fun getApplicant(
        @Parameter(hidden = true) @AuthUser user: User,
    ): ResponseEntity<ApplicantResponse> {
        val applicantEntity: ApplicantEntity = applicantService.getApplicant(user)

        // ApplicantInfoResponse DTO로 재포장 해 반환
        return ResponseEntity.ok(ApplicantResponse.fromEntity(applicantEntity))
    }

    @PutMapping("/me")
    fun putApplicant(
        @Parameter(hidden = true) @AuthUser user: User,
        @Valid @RequestBody request: PutApplicantRequest,
    ): ResponseEntity<ApplicantResponse> {
        val applicantResponse: ApplicantResponse = applicantService.putApplicant(user, request)
        return ResponseEntity.ok(applicantResponse)
    }
}
