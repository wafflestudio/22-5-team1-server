package com.waffletoy.team1server.company.service

import com.waffletoy.team1server.auth.UserNotFoundException
import com.waffletoy.team1server.auth.UserRole
import com.waffletoy.team1server.auth.dto.User
import com.waffletoy.team1server.auth.service.AuthService
import com.waffletoy.team1server.company.dto.Company
import com.waffletoy.team1server.company.dto.CreateCompanyRequest
import com.waffletoy.team1server.company.dto.LinkVo
import com.waffletoy.team1server.company.dto.TagVo
import com.waffletoy.team1server.company.dto.UpdateCompanyRequest
import com.waffletoy.team1server.company.persistence.CompanyEntity
import com.waffletoy.team1server.company.persistence.CompanyRepository
import com.waffletoy.team1server.exceptions.NotAuthorizedException
import com.waffletoy.team1server.post.PostCompanyExistsException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CompanyService(
    private val companyRepository: CompanyRepository,
    private val authService: AuthService,
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
            // Update existing company
            val updatedEntity = updateCompanyEntityWithRequest(existingCompany, request)
            Company.fromEntity(updatedEntity)
        } else {
            // Create new company
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
                    companyInfoPDFLink = request.companyInfoPDFLink,
                    landingPageLink = request.landingPageLink,
                    vcName = request.vcName,
                    vcRec = request.vcRec,
                    links = request.links.map { LinkVo(description = it.description, link = it.link) }.toMutableList(),
                    tags = request.tags.map { TagVo(tag = it.tag) }.toMutableList(),
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
        entity.companyInfoPDFLink = request.companyInfoPDFLink
        entity.landingPageLink = request.landingPageLink
        entity.vcName = request.vcName
        entity.vcRec = request.vcRec
        entity.links = request.links.map { LinkVo(description = it.description, link = it.link) }.toMutableList()
        entity.tags = request.tags.map { TagVo(tag = it.tag) }.toMutableList()
        return entity
    }

    @Transactional
    fun getCompanyByCompany(user: User): List<Company> {
        if (user.userRole != UserRole.COMPANY) {
            throw NotAuthorizedException()
        }

        val userEntity =
            authService.getUserEntityByUserId(user.id)
                ?: throw UserNotFoundException(mapOf("userId" to user.id))

        return companyRepository.findAllByUser(userEntity).map { Company.fromEntity(it) }
    }
}
