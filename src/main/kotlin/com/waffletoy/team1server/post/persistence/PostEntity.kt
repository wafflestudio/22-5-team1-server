package com.waffletoy.team1server.post.persistence

import com.waffletoy.team1server.account.persistence.AdminEntity
import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "posts")
open class PostEntity(
    @Id
    @Column(name = "ID", nullable = false)
    open val id: String = UUID.randomUUID().toString(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ADMIN_ID", nullable = false)
    open val admin: AdminEntity,
    @Column(name = "CREATED_AT", nullable = false)
    open val createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "UPDATED_AT", nullable = false)
    open var updatedAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "TITLE", nullable = false, length = 255)
    open val title: String,
    @Column(name = "NAME", nullable = false, length = 255)
    open val companyName: String,
    @Column(name = "EXPLANATION", columnDefinition = "TEXT")
    open val explanation: String? = null,
    @Column(name = "CONTENT", columnDefinition = "TEXT")
    open val content: String? = null,
    @Column(name = "EMAIL")
    open val email: String? = null,
    @Column(name = "IMAGE_LINK")
    open val imageLink: String? = null,
    @Column(name = "INVEST_AMOUNT")
    open val investAmount: Int = 0,
    @Column(name = "INVEST_COMPANY")
    open val investCompany: String? = null,
    @Column(name = "IR_DECK_LINK")
    open val irDeckLink: String? = null,
    @Column(name = "LANDING_PAGE_LINK")
    open val landingPageLink: String? = null,
    @Column(name = "EMPLOYMENT_END_DATE", nullable = false)
    open val employmentEndDate: LocalDateTime = LocalDateTime.now(),
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    // LINKS 테이블의 POST_ID 외래 키를 매핑
    @JoinColumn(name = "POST_ID")
    open val links: List<LinkEntity> = mutableListOf(),
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    // ROLES 테이블의 POST_ID 외래 키를 매핑
    @JoinColumn(name = "POST_ID")
    open val roles: List<RoleEntity> = mutableListOf(),
    @ManyToMany
    @JoinTable(
        // 중간 테이블 명
        name = "post_tags",
        // POST_ID와 매핑
        joinColumns = [JoinColumn(name = "post_id")],
        // TAG_ID와 매핑
        inverseJoinColumns = [JoinColumn(name = "tag_id")],
    )
    val tags: MutableSet<TagEntity> = mutableSetOf(),
    @Column(name = "IS_ACTIVE")
    open val isActive: Boolean = false,
)
