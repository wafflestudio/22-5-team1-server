package com.waffletoy.team1server.applicant.service

import com.waffletoy.team1server.applicant.ApplicantNotFoundException
import com.waffletoy.team1server.applicant.ApplicantUserForbiddenException
import com.waffletoy.team1server.applicant.dto.ApplicantResponse
import com.waffletoy.team1server.applicant.dto.PutApplicantRequest
import com.waffletoy.team1server.applicant.persistence.ApplicantEntity
import com.waffletoy.team1server.applicant.persistence.ApplicantRepository
import com.waffletoy.team1server.auth.UserNotFoundException
import com.waffletoy.team1server.auth.UserRole
import com.waffletoy.team1server.auth.dto.User
import com.waffletoy.team1server.auth.persistence.UserEntity
import com.waffletoy.team1server.auth.persistence.UserRepository
import org.springframework.context.annotation.Lazy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ApplicantService(
    val applicantRepository: ApplicantRepository,
    @Lazy private val userRepository: UserRepository,
) {
    fun getApplicant(
        user: User,
    ): ApplicantEntity {
        // User가 Applicant가 맞는지 확인
        if (user.userRole != UserRole.APPLICANT) {
            throw ApplicantUserForbiddenException(
                details = mapOf("userId" to user.id, "userRole" to user.userRole),
            )
        }

        // User의 Applicant 정보 받기
        val applicantEntity: ApplicantEntity? = applicantRepository.findByUserId(user.id)

        // applicant null이면 정보 없음 반환
        if (applicantEntity == null) {
            throw ApplicantNotFoundException()
        }

        // 정보 반환
        return applicantEntity
    }

    fun putApplicant(
        user: User,
        request: PutApplicantRequest,
    ): ApplicantResponse {
        // user가 Applicant 맞는지 확인
        if (user.userRole != UserRole.APPLICANT) {
            throw ApplicantUserForbiddenException(
                details = mapOf("userId" to user.id, "userRole" to user.userRole),
            )
        }

        val userEntity: UserEntity? = userRepository.findByIdOrNull(user.id)
        if (userEntity == null) {
            throw UserNotFoundException()
        }

        val applicantEntity: ApplicantEntity? = applicantRepository.findByUserId(user.id)

        var updatedApplicant =
            applicantEntity?.apply {
                updatedAt = LocalDateTime.now()
                enrollYear = request.enrollYear
                dept = request.department
                positions = request.positions
                slogan = request.slogan
                explanation = request.explanation
                stacks = request.stacks
                profileImageKey = request.imageKey
                cvKey = request.cvKey
                portfolioKey = request.portfolioKey
                links = request.links
            } ?: ApplicantEntity(
                user = userEntity,
                enrollYear = request.enrollYear,
                dept = request.department,
                positions = request.positions,
                slogan = request.slogan,
                explanation = request.explanation,
                stacks = request.stacks,
                profileImageKey = request.imageKey,
                cvKey = request.cvKey,
                portfolioKey = request.portfolioKey,
                links = request.links,
            )

        updatedApplicant = applicantRepository.saveAndFlush(updatedApplicant)

        return ApplicantResponse.fromEntity(updatedApplicant)
    }
}
