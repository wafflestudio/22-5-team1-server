package com.wafflestudio.internhasha.company.service

import com.wafflestudio.internhasha.auth.UserNotFoundException
import com.wafflestudio.internhasha.auth.UserRole
import com.wafflestudio.internhasha.auth.dto.User
import com.wafflestudio.internhasha.auth.service.AuthService
import com.wafflestudio.internhasha.company.CompanyNotFoundException
import com.wafflestudio.internhasha.company.dto.Company
import com.wafflestudio.internhasha.company.dto.CreateCompanyRequest
import com.wafflestudio.internhasha.company.dto.LinkVo
import com.wafflestudio.internhasha.company.dto.TagVo
import com.wafflestudio.internhasha.company.dto.UpdateCompanyRequest
import com.wafflestudio.internhasha.company.persistence.CompanyEntity
import com.wafflestudio.internhasha.company.persistence.CompanyRepository
import com.wafflestudio.internhasha.exceptions.NotAuthorizedException
import com.wafflestudio.internhasha.s3.service.S3Service
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompanyService(
    private val companyRepository: CompanyRepository,
    private val authService: AuthService,
    private val s3Service: S3Service,
) {
    /**
     * Creates a new company associated with the given user.
     *
     * @param user The authenticated user creating the company.
     * @param request The DTO containing company creation data.
     * @return The created Company DTO.
     * @throws NotAuthorizedException If user is not company.
     * @throws PostCompanyExistsException If a company with the given email already exists.
     */
    @Transactional
    fun putCompany(
        user: User,
        request: CreateCompanyRequest,
    ): Company {
        if (user.userRole != UserRole.COMPANY) {
            throw NotAuthorizedException()
        }

        val userEntity =
            authService.getUserEntityByUserId(user.id)
                ?: throw UserNotFoundException(mapOf("userId" to user.id))

        val existingCompany = companyRepository.findAllByUser(userEntity).firstOrNull()

        return if (existingCompany != null) {
            // 기존 s3 object 삭제
            existingCompany?.let { company ->
                company.companyInfoPDFKey?.let { if (it != request.companyInfoPDFKey) s3Service.deleteS3File(it) }
                company.profileImageKey?.let { if (it != request.profileImageKey) s3Service.deleteS3File(it) }
            }

            // Update existing company
            val updatedEntity = updateCompanyEntityWithRequest(existingCompany, request)
            Company.fromEntity(updatedEntity)
        } else {
            // Create new company -> AuthAPI를 통해 Company 계정을 생성하는 과정에서 Company
            val newEntity =
                CompanyEntity(
                    user = userEntity,
                    companyEstablishedYear = request.companyEstablishedYear,
                    domain = request.domain,
                    headcount = request.headcount,
                    location = request.location,
                    slogan = request.slogan,
                    detail = request.detail,
                    profileImageKey = request.profileImageKey,
                    companyInfoPDFKey = request.companyInfoPDFKey,
                    landingPageLink = request.landingPageLink,
                    links = request.links?.map { LinkVo(description = it.description, link = it.link) }?.toMutableList() ?: mutableListOf(),
                    tags = request.tags?.map { TagVo(tag = it.tag) }?.toMutableList() ?: mutableListOf(),
                )

            val saved = companyRepository.save(newEntity)
            Company.fromEntity(saved)
        }
    }

    private fun updateCompanyEntityWithRequest(
        entity: CompanyEntity,
        request: UpdateCompanyRequest,
    ): CompanyEntity {
        entity.companyEstablishedYear = request.companyEstablishedYear
        entity.domain = request.domain
        entity.headcount = request.headcount
        entity.location = request.location
        entity.slogan = request.slogan
        entity.detail = request.detail
        entity.profileImageKey = request.profileImageKey
        entity.companyInfoPDFKey = request.companyInfoPDFKey
        entity.landingPageLink = request.landingPageLink
        entity.links = request.links?.map { LinkVo(description = it.description, link = it.link) }?.toMutableList() ?: mutableListOf()
        entity.tags = request.tags?.map { TagVo(tag = it.tag) }?.toMutableList() ?: mutableListOf()
        return entity
    }

    @Transactional
    fun getCompany(user: User): Company {
        if (user.userRole != UserRole.COMPANY) {
            throw NotAuthorizedException()
        }

        val userEntity =
            authService.getUserEntityByUserId(user.id)
                ?: throw UserNotFoundException(mapOf("userId" to user.id))

        val companyEntity =
            companyRepository.findAllByUser(userEntity).firstOrNull() ?: throw CompanyNotFoundException(
                details = mapOf("userEntity" to userEntity),
            )

        return Company.fromEntity(companyEntity)
    }
}
