package com.waffletoy.team1server.coffeeChat.service

import com.waffletoy.team1server.coffeeChat.*
import com.waffletoy.team1server.coffeeChat.controller.*
import com.waffletoy.team1server.coffeeChat.dto.*
import com.waffletoy.team1server.coffeeChat.persistence.CoffeeChatEntity
import com.waffletoy.team1server.coffeeChat.persistence.CoffeeChatRepository
import com.waffletoy.team1server.post.persistence.PositionEntity
import com.waffletoy.team1server.post.service.PostService
import com.waffletoy.team1server.user.UserRole
import com.waffletoy.team1server.user.dtos.User
import com.waffletoy.team1server.user.persistence.UserEntity
import com.waffletoy.team1server.user.service.UserService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CoffeeChatService(
    private val coffeeChatRepository: CoffeeChatRepository,
    @Lazy private val userService: UserService,
    @Lazy private val postService: PostService,
) {
    @Value("\${custom.page.size:12}")
    private val pageSize: Int = 12

    fun getCoffeeChatDetailApplicant(
        user: User,
        coffeeChatId: String,
    ): CoffeeChatApplicant {
        // 커피챗 찾기
        val coffeeChatEntity = getCoffeeChatEntity(coffeeChatId)
        // 작성자가 아니면 403
        checkCoffeeChatAuthority(coffeeChatEntity, user, UserRole.NORMAL)
        return CoffeeChatApplicant.fromEntity(coffeeChatEntity)
    }

    fun getCoffeeChatDetailCompany(
        user: User,
        coffeeChatId: String,
    ): CoffeeChatCompany {
        // 커피챗 찾기
        val coffeeChatEntity = getCoffeeChatEntity(coffeeChatId)
        // 대상 회사가 아니면 403
        checkCoffeeChatAuthority(coffeeChatEntity, user, UserRole.CURATOR)
        return CoffeeChatCompany.fromEntity(coffeeChatEntity)
    }

    @Transactional
    fun applyCoffeeChat(
        user: User,
        postId: String,
        coffeeChatRequest: CoffeeChatRequest,
    ): CoffeeChatApplicant {
        if (user.userRole != UserRole.NORMAL) {
            throw CoffeeChatUserForbiddenException(
                details = mapOf("userId" to user.id, "userRole" to user.userRole),
            )
        }
        val userEntity = getUserEntityOrThrow(user.id)
        val positionEntity = getPositionEntityOrThrow(postId)

        // 지원 가능한지 마감시간을 확인
        if (positionEntity.employmentEndDate != null && LocalDateTime.now().isAfter(positionEntity.employmentEndDate!!)) {
            throw CoffeeChatPostExpiredException(
                details = mapOf("postId" to postId, "endDate" to positionEntity.employmentEndDate.toString()),
            )
        }

        val coffeeChatEntity =
            try {
                coffeeChatRepository.save(
                    CoffeeChatEntity(
                        content = coffeeChatRequest.content,
                        position = positionEntity,
                        applicant = userEntity,
                    ),
                )
            } catch (ex: Exception) {
                throw CoffeeChatCreationFailedException(
                    details =
                        mapOf(
                            "userId" to user.id,
                            "postId" to postId,
                            "error" to ex.message.orEmpty(),
                        ),
                )
            }

        return CoffeeChatApplicant.fromEntity(coffeeChatEntity)
    }

    @Transactional
    fun editCoffeeChat(
        user: User,
        coffeeChatId: String,
        coffeeChatRequest: CoffeeChatRequest,
    ): CoffeeChatApplicant {
        // 커피챗 찾기
        val coffeeChatEntity = getCoffeeChatEntity(coffeeChatId)
        // 작성자가 아니면 403
        checkCoffeeChatAuthority(coffeeChatEntity, user, UserRole.NORMAL)

        // 대기 중인 커피챗만 수정 가능
        if (coffeeChatEntity.coffeeChatStatus != CoffeeChatStatus.WAITING) {
            throw CoffeeChatStatusForbiddenException(
                details =
                    mapOf(
                        "coffeeChatId" to coffeeChatId,
                        "status" to coffeeChatEntity.coffeeChatStatus.toString(),
                    ),
            )
        }

        // 업데이트
        coffeeChatEntity.content = coffeeChatRequest.content

        return CoffeeChatApplicant.fromEntity(coffeeChatEntity)
    }

    @Transactional
    fun cancelCoffeeChat(
        user: User,
        coffeeChatId: String,
    ): CoffeeChatApplicant {
        // 커피챗 찾기
        val coffeeChatEntity = getCoffeeChatEntity(coffeeChatId)
        // 작성자가 아니면 403
        checkCoffeeChatAuthority(coffeeChatEntity, user, UserRole.NORMAL)
        // 업데이트
        if (coffeeChatEntity.coffeeChatStatus == CoffeeChatStatus.WAITING) {
            coffeeChatEntity.coffeeChatStatus = CoffeeChatStatus.CANCELED
        } else {
            throw CoffeeChatStatusForbiddenException(
                details =
                    mapOf(
                        "coffeeChatId" to coffeeChatId,
                        "status" to coffeeChatEntity.coffeeChatStatus.toString(),
                    ),
            )
        }
        return CoffeeChatApplicant.fromEntity(coffeeChatEntity)
    }

    @Transactional
    fun confirmCoffeeChat(
        user: User,
        coffeeChatId: String,
    ): CoffeeChatCompany {
        // 커피챗 찾기
        val coffeeChatEntity = getCoffeeChatEntity(coffeeChatId)
        // 대상 회사가 아니면 403
        checkCoffeeChatAuthority(coffeeChatEntity, user, UserRole.CURATOR)
        // 업데이트
        if (coffeeChatEntity.coffeeChatStatus == CoffeeChatStatus.WAITING) {
            coffeeChatEntity.coffeeChatStatus = CoffeeChatStatus.CONFIRMED
            coffeeChatEntity.changed = true
        } else {
            throw CoffeeChatStatusForbiddenException(
                details =
                    mapOf(
                        "coffeeChatId" to coffeeChatId,
                        "status" to coffeeChatEntity.coffeeChatStatus.toString(),
                    ),
            )
        }
        return CoffeeChatCompany.fromEntity(coffeeChatEntity)
    }

    @Transactional
    fun rejectCoffeeChat(
        user: User,
        coffeeChatId: String,
    ): CoffeeChatCompany {
        // 커피챗 찾기
        val coffeeChatEntity = getCoffeeChatEntity(coffeeChatId)
        // 대상 회사가 아니면 403
        checkCoffeeChatAuthority(coffeeChatEntity, user, UserRole.CURATOR)
        // 업데이트
        if (coffeeChatEntity.coffeeChatStatus == CoffeeChatStatus.WAITING) {
            coffeeChatEntity.coffeeChatStatus = CoffeeChatStatus.REJECTED
            coffeeChatEntity.changed = true
        } else {
            throw CoffeeChatStatusForbiddenException(
                details =
                    mapOf(
                        "coffeeChatId" to coffeeChatId,
                        "status" to coffeeChatEntity.coffeeChatStatus.toString(),
                    ),
            )
        }
        return CoffeeChatCompany.fromEntity(coffeeChatEntity)
    }

    @Transactional
    fun getCoffeeChatListApplicant(
        user: User,
    ): List<CoffeeChatBrief> {
        if (user.userRole != UserRole.NORMAL) {
            throw CoffeeChatUserForbiddenException(
                details = mapOf("userId" to user.id, "userRole" to user.userRole),
            )
        }
        val coffeeChatEntityList = coffeeChatRepository.findAllByApplicantId(user.id)
        // 커피챗 DTO를 준비(changed 반영)
        val ret = coffeeChatEntityList.map { CoffeeChatBrief.fromEntity(it) }
        // changed를 false로 수정
        coffeeChatEntityList.forEach { it.changed = false }
        return ret
    }

    @Transactional
    fun getCoffeeChatListCompany(
        user: User,
    ): List<CoffeeChatBrief> {
        if (user.userRole != UserRole.CURATOR) {
            throw CoffeeChatUserForbiddenException(
                details = mapOf("userId" to user.id, "userRole" to user.userRole),
            )
        }
        return coffeeChatRepository.findAllExceptStatusByCuratorId(user.id, CoffeeChatStatus.CANCELED)
            .map { CoffeeChatBrief.fromEntity(it) }
    }

    fun countChangedCoffeeChatApplicant(
        user: User,
    ): Int {
        if (user.userRole != UserRole.NORMAL) {
            throw CoffeeChatUserForbiddenException(
                details = mapOf("userId" to user.id, "userRole" to user.userRole),
            )
        }

        return coffeeChatRepository.countByApplicantIdAndChangedTrue(
            user.id,
        ).toInt()
    }

    fun countWaitingCoffeeChatsCompany(
        user: User,
    ): Int {
        if (user.userRole != UserRole.CURATOR) {
            throw CoffeeChatUserForbiddenException(
                details = mapOf("userId" to user.id, "userRole" to user.userRole),
            )
        }

        return coffeeChatRepository.countByCuratorIdAndStatus(
            curatorId = user.id,
            status = CoffeeChatStatus.WAITING,
        ).toInt()
    }

    fun getCoffeeChatEntity(coffeeChatId: String): CoffeeChatEntity =
        coffeeChatRepository.findByIdOrNull(coffeeChatId)
            ?: throw CoffeeChatNotFoundException(
                details = mapOf("coffeeChatId" to coffeeChatId),
            )

    fun checkCoffeeChatAuthority(
        coffeeChatEntity: CoffeeChatEntity,
        user: User,
        userRole: UserRole,
    ) {
        if (user.userRole != userRole) {
            throw CoffeeChatUserForbiddenException(
                details = mapOf("userId" to user.id, "userRole" to user.userRole),
            )
        }
        when (user.userRole) {
            UserRole.NORMAL -> {
                if (coffeeChatEntity.applicant.id != user.id) {
                    throw CoffeeChatUserForbiddenException(
                        details = mapOf("userId" to user.id, "userRole" to user.userRole),
                    )
                }
            }
            UserRole.CURATOR -> {
                if (coffeeChatEntity.position.company.curator.id != user.id) {
                    throw CoffeeChatUserForbiddenException(
                        details = mapOf("userId" to user.id, "userRole" to user.userRole),
                    )
                }
            }
        }
    }

    fun getPositionEntityOrThrow(postId: String): PositionEntity =
        postService.getPositionEntityByPostId(postId) ?: throw CoffeeChatNotFoundException(
            details = mapOf("postId" to postId),
        )

    fun getUserEntityOrThrow(userId: String): UserEntity =
        userService.getUserEntityByUserId(userId) ?: throw CoffeeChatNotFoundException(
            details = mapOf("userId" to userId),
        )

    // normal 유저 탈퇴 시 bookmark 데이터를 삭제
    // curator 유저가 작성한 company, position 데이터는 유지
    @Transactional(propagation = Propagation.REQUIRED)
    fun deleteCoffeeChatByUser(userEntity: UserEntity) {
        coffeeChatRepository.deleteAllByApplicantId(userEntity.id)
    }
}
