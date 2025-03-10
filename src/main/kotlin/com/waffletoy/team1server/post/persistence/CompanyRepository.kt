package com.waffletoy.team1server.post.persistence

import com.waffletoy.team1server.user.persistence.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CompanyRepository : JpaRepository<CompanyEntity, String> {
    /**
     * Checks if a company with the given email exists.
     *
     * @param email The email to check.
     * @return True if a company with the email exists, else false.
     */
    fun existsByEmail(email: String): Boolean

    fun findAllByCompany(company: UserEntity): List<CompanyEntity>
}
