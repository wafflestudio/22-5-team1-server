package com.waffletoy.team1server.applicant.persistence

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.waffletoy.team1server.applicant.dto.Link
import com.waffletoy.team1server.auth.UserRole
import com.waffletoy.team1server.auth.persistence.UserEntity
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@Entity(name = "applicants")
@EntityListeners(AuditingEntityListener::class)
class ApplicantEntity(
    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    val id: String = UUID.randomUUID().toString(),
    @OneToOne
    @JoinColumn(name = "user_id")
    val user: UserEntity,
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    val userRole: UserRole = UserRole.APPLICANT,
    // MySQL SMALLINT는 -32768~32767
    @Column(name = "enroll_year", columnDefinition = "SMALLINT")
    var enrollYear: Int? = null,
    @Column(name = "dept")
    var dept: String? = null,
    @Column(name = "positions")
    @Convert(converter = StringListConverter::class)
    var positions: List<String>? = null,
    @Column(name = "slogan")
    var slogan: String? = null,
    @Column(name = "explanation", columnDefinition = "TEXT")
    var explanation: String? = null,
    @Column(name = "stacks", length = 512)
    @Convert(converter = StringListConverter::class)
    var stacks: List<String>? = null,
    @Column(name = "profile_image_key")
    var profileImageKey: String? = null,
    @Column(name = "cv_key")
    var cvKey: String? = null,
    @Column(name = "portfolio_key")
    var portfolioKey: String? = null,
    @Column(name = "links", length = 10500)
    @Convert(converter = LinkListConverter::class)
    var links: List<Link>? = null,
)

@Converter(autoApply = false)
class StringListConverter : AttributeConverter<List<String>?, String?> {
    private val objectMapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: List<String>?): String? {
        return attribute?.let { objectMapper.writeValueAsString(it) }
    }

    override fun convertToEntityAttribute(str: String?): List<String>? {
        return str?.let {
            objectMapper.readValue(it, object : TypeReference<List<String>>() {})
        }
    }
}

@Converter(autoApply = false)
class LinkListConverter : AttributeConverter<List<Link>?, String?> {
    private val objectMapper = jacksonObjectMapper()

    override fun convertToDatabaseColumn(attribute: List<Link>?): String? {
        return attribute?.let { objectMapper.writeValueAsString(it) }
    }

    override fun convertToEntityAttribute(str: String?): List<Link>? {
        return str?.let {
            objectMapper.readValue(it, object : TypeReference<List<Link>>() {})
        }
    }
}
