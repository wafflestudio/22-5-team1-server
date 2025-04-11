package com.waffletoy.team1server.coffeeChat.service

import com.waffletoy.team1server.auth.UserRole
import com.waffletoy.team1server.auth.dto.User
import com.waffletoy.team1server.auth.persistence.UserEntity
import com.waffletoy.team1server.auth.service.AuthService
import com.waffletoy.team1server.coffeeChat.*
import com.waffletoy.team1server.coffeeChat.controller.*
import com.waffletoy.team1server.coffeeChat.dto.*
import com.waffletoy.team1server.coffeeChat.persistence.CoffeeChatEntity
import com.waffletoy.team1server.coffeeChat.persistence.CoffeeChatRepository
import com.waffletoy.team1server.email.EmailType
import com.waffletoy.team1server.email.service.EmailService
import com.waffletoy.team1server.post.PostNotFoundException
import com.waffletoy.team1server.post.PostPositionNotFoundException
import com.waffletoy.team1server.post.persistence.PositionEntity
import com.waffletoy.team1server.post.service.PostService
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
    @Lazy private val authService: AuthService,
    @Lazy private val postService: PostService,
    @Lazy private val emailService: EmailService,
) {
    @Value("\${custom.page.size:12}")
    private val pageSize: Int = 12

    fun getCoffeeChatDetail(
        user: User,
        coffeeChatId: String,
    ): CoffeeChatDetail {
        return when (user.userRole) {
            UserRole.APPLICANT -> getCoffeeChatDetailApplicant(user, coffeeChatId)
            UserRole.COMPANY -> getCoffeeChatDetailCompany(user, coffeeChatId)
        }
    }

    fun checkIsSubmitted(
        user: User,
        postId: String,
    ): Boolean {
        if (user.userRole != UserRole.APPLICANT) {
            throw CoffeeChatUserForbiddenException(details = mapOf("userId" to user.id, "userRole" to user.userRole))
        }

        getPositionEntityOrThrow(postId)

        // 이미 대기 중인 커피챗 ID가 있으면 True
        return (
            coffeeChatRepository.findByApplicantIdAndPositionIdAndCoffeeChatStatus(
                user.id, postId, CoffeeChatStatus.WAITING,
            ) != null
        )
    }

    private fun getCoffeeChatDetailApplicant(
        user: User,
        coffeeChatId: String,
    ): CoffeeChatApplicant {
        // 커피챗 찾기
        val coffeeChatEntity = getCoffeeChatEntity(coffeeChatId)
        // 작성자가 아니면 403
        checkCoffeeChatAuthority(coffeeChatEntity, user, UserRole.APPLICANT)
        return CoffeeChatApplicant.fromEntity(coffeeChatEntity)
    }

    private fun getCoffeeChatDetailCompany(
        user: User,
        coffeeChatId: String,
    ): CoffeeChatCompany {
        // 커피챗 찾기
        val coffeeChatEntity = getCoffeeChatEntity(coffeeChatId)
        // 대상 회사가 아니면 403
        checkCoffeeChatAuthority(coffeeChatEntity, user, UserRole.COMPANY)
        return CoffeeChatCompany.fromEntity(coffeeChatEntity)
    }

    @Transactional
    fun applyCoffeeChat(
        user: User,
        postId: String,
        coffeeChatContent: CoffeeChatContent,
    ): CoffeeChatApplicant {
        if (user.userRole != UserRole.APPLICANT) {
            throw CoffeeChatUserForbiddenException(
                details = mapOf("userId" to user.id, "userRole" to user.userRole),
            )
        }
        val userEntity = getUserEntityOrThrow(user.id)
        val positionEntity = getPositionEntityOrThrow(postId)

        // 이미 대기 중인 커피챗이 있는지 확인
        val existingCoffeeChat =
            coffeeChatRepository.findByApplicantIdAndPositionIdAndCoffeeChatStatus(
                userEntity.id,
                positionEntity.id,
                CoffeeChatStatus.WAITING,
            )
        if (existingCoffeeChat != null) {
            throw CoffeeChatDuplicationException(
                details = mapOf("coffeeChatId" to existingCoffeeChat.id, "coffeeChatStatus" to CoffeeChatStatus.WAITING),
            )
        }

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
                        content = coffeeChatContent.content,
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

        // 회사에 이메일 전송
        emailService.sendEmail(
            to = positionEntity.company.user.email,
            type = EmailType.Notification,
            subject = "[인턴하샤] 지원자 커피챗 안내",
            text = "",
            coffeeChatEntity = coffeeChatEntity,
        )

        return CoffeeChatApplicant.fromEntity(coffeeChatEntity)
    }

    @Transactional
    fun editCoffeeChat(
        user: User,
        coffeeChatId: String,
        coffeeChatContent: CoffeeChatContent,
    ): CoffeeChatApplicant {
        // 커피챗 찾기
        val coffeeChatEntity = getCoffeeChatEntity(coffeeChatId)
        // 작성자가 아니면 403
        checkCoffeeChatAuthority(coffeeChatEntity, user, UserRole.APPLICANT)

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
        coffeeChatEntity.content = coffeeChatContent.content

        return CoffeeChatApplicant.fromEntity(coffeeChatEntity)
    }

    @Transactional
    fun changeCoffeeChatStatus(
        user: User,
        coffeeChatStatusReq: CoffeeChatStatusReq,
    ): CoffeeChatDetailList {
        // 하나라도 유효하지 않은 커피챗이 들어오면 404
        val coffeeChatEntityList = coffeeChatStatusReq.coffeeChatList.map { getCoffeeChatEntity(it) }

        val succeeded: MutableList<CoffeeChatDetail> = mutableListOf()
        val failed: MutableList<CoffeeChatDetail> = mutableListOf()

        // 변경 권한을 확인
        when (user.userRole) {
            UserRole.APPLICANT -> {
                // 지원자는 취소만 가능
                if (coffeeChatStatusReq.coffeeChatStatus != CoffeeChatStatus.CANCELED) {
                    throw CoffeeChatUserForbiddenException(
                        details =
                            mapOf(
                                "userRole" to user.userRole,
                                "coffeeChatStatus" to coffeeChatStatusReq.coffeeChatStatus,
                            ),
                    )
                }
                // 본인이 작성한 커피챗만 & 대기 중인 커피챗만
                for (coffeeChatEntity in coffeeChatEntityList) {
                    if (coffeeChatEntity.applicant.id != user.id ||
                        coffeeChatEntity.coffeeChatStatus != CoffeeChatStatus.WAITING
                    ) {
                        failed.add(CoffeeChatApplicant.fromEntity(coffeeChatEntity))
                    } else {
                        coffeeChatEntity.coffeeChatStatus = coffeeChatStatusReq.coffeeChatStatus
                        coffeeChatEntity.changed = true
                        succeeded.add(CoffeeChatApplicant.fromEntity(coffeeChatEntity))
                    }
                }
            }
            UserRole.COMPANY -> {
                // 회사는 수락, 거절만 가능
                if (coffeeChatStatusReq.coffeeChatStatus != CoffeeChatStatus.ACCEPTED &&
                    coffeeChatStatusReq.coffeeChatStatus != CoffeeChatStatus.REJECTED
                ) {
                    throw CoffeeChatUserForbiddenException(
                        details =
                            mapOf(
                                "userRole" to user.userRole,
                                "coffeeChatStatus" to coffeeChatStatusReq.coffeeChatStatus,
                            ),
                    )
                }
                // 자신이 작성한 공고의 커피챗만 & 대기 중인 커피챗만
                for (coffeeChatEntity in coffeeChatEntityList) {
                    if (coffeeChatEntity.position.company.user.id != user.id ||
                        coffeeChatEntity.coffeeChatStatus != CoffeeChatStatus.WAITING
                    ) {
                        failed.add(CoffeeChatCompany.fromEntity(coffeeChatEntity))
                    } else {
                        coffeeChatEntity.coffeeChatStatus = coffeeChatStatusReq.coffeeChatStatus
                        coffeeChatEntity.changed = true
                        succeeded.add(CoffeeChatCompany.fromEntity(coffeeChatEntity))
                    }
                }
            }
        }
        return CoffeeChatDetailList(
            succeeded = succeeded,
            failed = failed,
        )
    }

//    private fun cancelCoffeeChat(
//        user: User,
//        coffeeChatId: String,
//    ): CoffeeChatApplicant {
//        // 커피챗 찾기
//        val coffeeChatEntity = getCoffeeChatEntity(coffeeChatId)
//        // 작성자가 아니면 403
//        checkCoffeeChatAuthority(coffeeChatEntity, user, UserRole.APPLICANT)
//        // 업데이트
//        if (coffeeChatEntity.coffeeChatStatus == CoffeeChatStatus.WAITING) {
//            coffeeChatEntity.coffeeChatStatus = CoffeeChatStatus.CANCELED
//        } else {
//            throw CoffeeChatStatusForbiddenException(
//                details =
//                    mapOf(
//                        "coffeeChatId" to coffeeChatId,
//                        "status" to coffeeChatEntity.coffeeChatStatus.toString(),
//                    ),
//            )
//        }
//        return CoffeeChatApplicant.fromEntity(coffeeChatEntity)
//    }
//
//    private fun acceptCoffeeChat(
//        user: User,
//        coffeeChatId: String,
//    ): CoffeeChatCompany {
//        // 커피챗 찾기
//        val coffeeChatEntity = getCoffeeChatEntity(coffeeChatId)
//        // 대상 회사가 아니면 403
//        checkCoffeeChatAuthority(coffeeChatEntity, user, UserRole.COMPANY)
//        // 업데이트
//        if (coffeeChatEntity.coffeeChatStatus == CoffeeChatStatus.WAITING) {
//            coffeeChatEntity.coffeeChatStatus = CoffeeChatStatus.ACCEPTED
//            coffeeChatEntity.changed = true
//        } else {
//            throw CoffeeChatStatusForbiddenException(
//                details =
//                    mapOf(
//                        "coffeeChatId" to coffeeChatId,
//                        "status" to coffeeChatEntity.coffeeChatStatus.toString(),
//                    ),
//            )
//        }
//        return CoffeeChatCompany.fromEntity(coffeeChatEntity)
//    }
//
//    private fun rejectCoffeeChat(
//        user: User,
//        coffeeChatId: String,
//    ): CoffeeChatCompany {
//        // 커피챗 찾기
//        val coffeeChatEntity = getCoffeeChatEntity(coffeeChatId)
//        // 대상 회사가 아니면 403
//        checkCoffeeChatAuthority(coffeeChatEntity, user, UserRole.COMPANY)
//        // 업데이트
//        if (coffeeChatEntity.coffeeChatStatus == CoffeeChatStatus.WAITING) {
//            coffeeChatEntity.coffeeChatStatus = CoffeeChatStatus.REJECTED
//            coffeeChatEntity.changed = true
//        } else {
//            throw CoffeeChatStatusForbiddenException(
//                details =
//                    mapOf(
//                        "coffeeChatId" to coffeeChatId,
//                        "status" to coffeeChatEntity.coffeeChatStatus.toString(),
//                    ),
//            )
//        }
//        return CoffeeChatCompany.fromEntity(coffeeChatEntity)
//    }

    @Transactional
    fun getCoffeeChatListApplicant(
        user: User,
    ): List<CoffeeChatBrief> {
        if (user.userRole != UserRole.APPLICANT) {
            throw CoffeeChatUserForbiddenException(
                details = mapOf("userId" to user.id, "userRole" to user.userRole),
            )
        }
        val coffeeChatEntityList = coffeeChatRepository.findAllByApplicantId(user.id)

        // 커피챗 DTO를 준비(changed 반영)
        val ret = coffeeChatEntityList.map { CoffeeChatBrief.fromEntity(it) }

        // changed 값을 false로 변경, 저장
        coffeeChatEntityList.filter { it.changed }.forEach {
            it.changed = false
            coffeeChatRepository.save(it)
        }
        return ret
    }

    fun getCoffeeChatListCompany(
        user: User,
    ): List<CoffeeChatBrief> {
        if (user.userRole != UserRole.COMPANY) {
            throw CoffeeChatUserForbiddenException(
                details = mapOf("userId" to user.id, "userRole" to user.userRole),
            )
        }
        return coffeeChatRepository.findAllExceptStatusByUserId(user.id, CoffeeChatStatus.CANCELED)
            .map { CoffeeChatBrief.fromEntity(it) }
    }

    fun countCoffeeChatBadges(
        user: User,
    ): Int {
        return when (user.userRole) {
            UserRole.APPLICANT ->
                coffeeChatRepository.countByApplicantIdAndChangedTrue(
                    applicantId = user.id,
                ).toInt()
            UserRole.COMPANY ->
                coffeeChatRepository.countByUserIdAndStatus(
                    userId = user.id,
                    status = CoffeeChatStatus.WAITING,
                ).toInt()
        }
    }

    // 커피챗 엔티티 가져오기(외부 사용 가능)
    fun getCoffeeChatEntity(coffeeChatId: String): CoffeeChatEntity =
        coffeeChatRepository.findByIdOrNull(coffeeChatId)
            ?: throw CoffeeChatNotFoundException(
                details = mapOf("coffeeChatId" to coffeeChatId),
            )

    private fun checkCoffeeChatAuthority(
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
            UserRole.APPLICANT -> {
                if (coffeeChatEntity.applicant.id != user.id) {
                    throw CoffeeChatUserForbiddenException(
                        details = mapOf("userId" to user.id, "userRole" to user.userRole),
                    )
                }
            }
            UserRole.COMPANY -> {
                if (coffeeChatEntity.position.company.user.id != user.id) {
                    throw CoffeeChatUserForbiddenException(
                        details = mapOf("userId" to user.id, "userRole" to user.userRole),
                    )
                }
            }
        }
    }

    private fun getPositionEntityOrThrow(postId: String): PositionEntity =
        postService.getPositionEntityByPostId(postId) ?: throw PostNotFoundException(
            details = mapOf("postId" to postId),
        )

    private fun getUserEntityOrThrow(userId: String): UserEntity =
        authService.getUserEntityByUserId(userId) ?: throw CoffeeChatNotFoundException(
            details = mapOf("userId" to userId),
        )

    // 지원자 탈퇴 시 coffeeChat 데이터를 삭제
    @Transactional(propagation = Propagation.REQUIRED)
    fun deleteCoffeeChatByUser(userEntity: UserEntity) {
        coffeeChatRepository.deleteAllByApplicantId(userEntity.id)
    }

    // 특정 공고에 대해 커피챗 개수 가져오기(외부 사용 가능)
    fun getCoffeeChatCount(positionId: String): Long =
        coffeeChatRepository.countByPositionId(positionId)
            ?: throw PostPositionNotFoundException(
                details = mapOf("positionId" to positionId),
            )
}
