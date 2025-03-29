package com.waffletoy.team1server.company.dto

import com.waffletoy.team1server.company.persistence.CompanyEntity
import java.time.LocalDateTime

class Company(
    val id: String,
    // 필수 정보
    val companyName: String,
    val companyEstablishedYear: Int,
    val domain: String,
    val headcount: Int,
    val location: String,
    val slogan: String,
    val detail: String,
    val profileImageKey: String,
    // 선택 정보
    val companyInfoPDFLink: String?,
    val landingPageLink: String,
    val links: List<Link>,
    val tags: List<Tag>,
    val vcName: String,
    val vcRec: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun fromEntity(entity: CompanyEntity): Company {
            return Company(
                id = entity.id,
                companyName = entity.user.name,
                companyEstablishedYear = entity.companyEstablishedYear,
                domain = entity.domain,
                headcount = entity.headcount,
                location = entity.location,
                slogan = entity.slogan,
                detail = entity.detail,
                profileImageKey = entity.profileImageKey,
                // Optional field
                companyInfoPDFLink = entity.companyInfoPDFLink,
                landingPageLink = entity.landingPageLink,
                vcName = entity.vcName,
                vcRec = entity.vcRec,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                links = entity.links.map { Link.fromVo(it) },
                tags = entity.tags.map { Tag.fromVo(it) },
//                positions = entity.positions.map { Position.fromEntity(it) },
            )
        }
    }
}
