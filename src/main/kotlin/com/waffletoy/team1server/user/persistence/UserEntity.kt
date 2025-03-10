package com.waffletoy.team1server.user.persistence

import com.waffletoy.team1server.user.UserRole
import jakarta.persistence.*
import jakarta.validation.ValidationException
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.*

@Entity(name = "users")
@EntityListeners(AuditingEntityListener::class)
class UserEntity(
    // Basic user information
    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "mail", nullable = false, unique = true)
    val mail: String,
    @Column(name = "password_hash", nullable = true)
    open var passwordHash: String? = null,
    // Date info
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null,
    // Role-based access control
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val userRole: UserRole,
    // APPLICATNT specific field
//    @Column(name = "phone_number", nullable = true)
//    var phoneNumber: String? = null,
    @Column(name = "profile_image_link", nullable = true, length = 2048)
    val profileImageLink: String? = null,
) {
    // Validates either local or google login is provided
    @PrePersist
    @PreUpdate
    fun validate() {
        if (passwordHash == null) {
            throw ValidationException("passwordHash must be non-null")
        }
        if (userRole == UserRole.APPLICANT && name.isNullOrBlank()) {
            throw ValidationException("User name cannot be blank")
        }
    }
}
