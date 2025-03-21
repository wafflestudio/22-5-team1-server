package com.waffletoy.team1server.applicant.service

import com.waffletoy.team1server.applicant.persistence.ApplicantEntity
import com.waffletoy.team1server.applicant.persistence.ApplicantRepository
import com.waffletoy.team1server.auth.UserRole
import com.waffletoy.team1server.auth.dto.User
import org.springframework.stereotype.Service

@Service
class ApplicantService(
    val applicantRepository: ApplicantRepository
) {
    fun getApplicant(
        user: User,
    ) : ApplicantEntity {
        // User가 Applicant가 맞는지 확인
        if ( user.userRole != UserRole.APPLICANT ) {
            TODO("NotApplicantError 반환")
        }

        // User의 Applicant 정보 받기
        val applicantEntity : ApplicantEntity? = applicantRepository.findByUser(user)

        // applicant null이면 정보 없음 반환
        if (applicantEntity == null) {
            TODO("ApplicantNotFoundError 반환")
        }

        // 정보 반환
        return applicantEntity
    }

}