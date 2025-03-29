package com.waffletoy.team1server.company.persistence

import com.waffletoy.team1server.auth.persistence.UserEntity
import com.waffletoy.team1server.company.dto.LinkVo
import com.waffletoy.team1server.company.dto.TagVo
import com.waffletoy.team1server.post.persistence.PositionEntity
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "companies")
@EntityListeners(AuditingEntityListener::class)
class CompanyEntity(
    @Id
    @Column(name = "ID", nullable = false)
    open val id: String = UUID.randomUUID().toString(),
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER", nullable = false)
    open val user: UserEntity,
    @Column(name = "ESTABLISHED_YEAR", nullable = false)
    open var companyEstablishedYear: Int,
    @Column(name = "DOMAIN", nullable = false)
    open var domain: String,
    @Column(name = "HEADCOUNT", nullable = false)
    open var headcount: Int,
    @Column(name = "LOCATION", nullable = false)
    open var location: String,
    @Column(name = "SLOGAN", nullable = false)
    open var slogan: String,
    @Column(name = "DETAIL", columnDefinition = "TEXT", nullable = false)
    open var detail: String,
    @Column(name = "PROFILE_IMAGE_KEY", nullable = true)
    open var profileImageKey: String,
    @Column(name = "COMPANY_INFO_PDF_LINK", length = 2048, nullable = true)
    open var companyInfoPDFLink: String? = null,
    @Column(name = "LANDING_PAGE_LINK", length = 2048, nullable = false)
    open var landingPageLink: String,
    @Column(name = "VC_NAME", nullable = true)
    open var vcName: String,
    @Column(name = "VC_REC", nullable = true)
    open var vcRec: String,
    @CreatedDate
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    open var createdAt: LocalDateTime = LocalDateTime.now(),
    @LastModifiedDate
    @Column(name = "UPDATED_AT", nullable = false)
    open var updatedAt: LocalDateTime = LocalDateTime.now(),
    // 링크를 value object로 관리 - 자동으로 테이블 생성
    @ElementCollection
    @CollectionTable(
        // 매핑될 테이블 이름
        name = "company_links",
        joinColumns = [JoinColumn(name = "company_id")],
    )
    open var links: MutableList<LinkVo> = mutableListOf(),
    // 태그를 value object로 관리 - 자동으로 테이블 생성
    @ElementCollection
    @CollectionTable(
        // 매핑될 테이블 이름
        name = "company_tags",
        joinColumns = [JoinColumn(name = "company_id")],
    )
    open var tags: MutableList<TagVo> = mutableListOf(),
    // Positions 테이블의 POST 외래 키를 매핑
    @OneToMany(mappedBy = "company", cascade = [CascadeType.ALL], orphanRemoval = true)
    open val positions: MutableList<PositionEntity> = mutableListOf(),
)
