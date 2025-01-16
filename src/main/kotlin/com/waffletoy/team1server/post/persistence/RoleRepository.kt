package com.waffletoy.team1server.post.persistence

import com.waffletoy.team1server.post.Category
import jakarta.persistence.criteria.Predicate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface RoleRepository : JpaRepository<RoleEntity, String>, JpaSpecificationExecutor<RoleEntity> {
    @Query("SELECT p FROM RoleEntity p WHERE p.id IN :ids")
    fun findAllByIdIn(
        @Param("ids") ids: List<String>,
        pageable: Pageable,
    ): Page<RoleEntity>
}

class RoleSpecification {
    companion object {
        fun withFilters(
            roles: List<String>?,
            investmentMax: Int?,
            investmentMin: Int?,
            status: Int,
            currentDateTime: LocalDateTime = LocalDateTime.now(),
        ): Specification<RoleEntity> {
            return Specification { root, query, criteriaBuilder ->

                val predicates = mutableListOf<Predicate>()

                // roles 조건
                roles?.let { it ->
                    val roleJoin = root.join<RoleEntity, RoleEntity>("roles")
                    val roleEnums = it.mapNotNull { roleName -> Category.entries.find { it.name == roleName } }
                    if (roleEnums.isNotEmpty()) {
                        predicates.add(
                            criteriaBuilder.or(
                                *roleEnums.map { roleEnum ->
                                    criteriaBuilder.equal(roleJoin.get<Category>("category"), roleEnum.name)
                                }.toTypedArray(),
                            ),
                        )
                    }
                }

                // investment 조건 (PostEntity와 조인)
                val postJoin = root.join<RoleEntity, CompanyEntity>("post")

                // 하한
                investmentMin?.let {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(postJoin.get<Int>("investAmount"), it))
                }

                // 상한
                investmentMax?.let {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(postJoin.get<Int>("investAmount"), it))
                }

                // status 조건
                status.let {
                    when (it) {
                        0 -> {
                            // 진행 중 (현재 날짜가 employmentEndDate 이전)
                            predicates.add(
                                criteriaBuilder.greaterThanOrEqualTo(root.get("employmentEndDate"), currentDateTime),
                            )
                        }
                        1 -> {
                            // 진행 완료 (현재 날짜가 employmentEndDate 이후)
                            predicates.add(
                                criteriaBuilder.lessThan(root.get("employmentEndDate"), currentDateTime),
                            )
                        }
                        2 -> {
                            // 전체 (조건 없음)
                        }

                        else -> {
                            throw IllegalArgumentException("Invalid status value: $it")
                        }
                    }
                }

                // 최종 조건 조합
                criteriaBuilder.and(*predicates.toTypedArray())
            }
        }
    }
}
