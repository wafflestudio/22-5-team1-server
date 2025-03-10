package com.waffletoy.team1server.user.service

import com.waffletoy.team1server.coffeeChat.service.CoffeeChatService
import com.waffletoy.team1server.email.EmailSendFailureException
import com.waffletoy.team1server.email.service.EmailService
import com.waffletoy.team1server.exceptions.*
import com.waffletoy.team1server.post.service.PostService
import com.waffletoy.team1server.user.*
import com.waffletoy.team1server.user.controller.*
import com.waffletoy.team1server.user.dtos.*
import com.waffletoy.team1server.user.persistence.UserEntity
import com.waffletoy.team1server.user.persistence.UserRepository
import com.waffletoy.team1server.user.utils.UserTokenUtil
import jakarta.transaction.Transactional
import org.mindrot.jbcrypt.BCrypt
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Lazy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Transactional
@Service
class UserService(
    private val userRepository: UserRepository,
    private val userRedisCacheService: UserRedisCacheService,
    private val googleOAuth2Client: GoogleOAuth2Client,
    @Lazy private val emailService: EmailService,
    @Lazy private val coffeeChatService: CoffeeChatService,
    @Lazy private val postService: PostService,
) {
    // Sign up functions
    fun checkDuplicateId(request: CheckDuplicateIdRequest) {
        if (userRepository.existsByLocalLoginId(request.id)) {
            throw UserDuplicateLocalIdException(
                details = mapOf("localLoginId" to request.id),
            )
        }
    }

    fun checkDuplicateSnuMail(request: CheckDuplicateSnuMailRequest) {
        if (userRepository.existsBySnuMail(request.snuMail)) {
            throw UserDuplicateSnuMailException(
                details = mapOf("snuMail" to request.snuMail),
            )
        }
    }

    @Transactional
    fun signUp(request: SignUpRequest): Pair<User, UserTokenUtil.Tokens> {
        val user: User =
            when (request.authType) {
                SignUpRequest.AuthType.LOCAL_NORMAL -> {
                    val info = request.info as SignUpRequest.LocalNormalInfo
                    localNormalSignUp(info)
                }

                SignUpRequest.AuthType.LOCAL_CURATOR -> {
                    val info = request.info as SignUpRequest.LocalCuratorInfo
                    localCuratorSignUp(info)
                }
            }
        val tokens = UserTokenUtil.generateTokens(user)

        // 발급 받은 refresh token을 redis에 저장합니다.
        userRedisCacheService.saveRefreshToken(user.id, tokens.refreshToken)

        return Pair(user, tokens)
    }

    private fun localNormalSignUp(info: SignUpRequest.LocalNormalInfo): User {
        var user = userRepository.findBySnuMail(info.snuMail)
        if (user != null) {
            if (user.userRole != UserRole.NORMAL) {
                throw NotAuthorizedException(
                    details = mapOf("userId" to user.id, "userRole" to user.userRole),
                )
            } else {
                throw UserDuplicateSnuMailException(
                    details = mapOf("snuMail" to info.snuMail),
                )
            }
        } else {
            if (userRepository.existsByLocalLoginId(info.localLoginId)) {
                throw UserDuplicateLocalIdException(
                    details = mapOf("localLoginId" to info.localLoginId),
                )
            }
            user =
                userRepository.save(
                    UserEntity(
                        snuMail = info.snuMail,
                        name = info.name,
                        localLoginId = info.localLoginId,
                        localLoginPasswordHash = BCrypt.hashpw(info.password, BCrypt.gensalt()),
                        userRole = UserRole.NORMAL,
                    ),
                )
        }
        return User.fromEntity(entity = user)
    }

    private fun localCuratorSignUp(info: SignUpRequest.LocalCuratorInfo): User {
        if (info.secretPassword != devSecret) {
            throw InvalidCredentialsException(
                details = mapOf("secretPassword" to info.secretPassword),
            )
        }

        if (userRepository.existsByLocalLoginId(info.localLoginId)) {
            throw UserDuplicateLocalIdException(
                details = mapOf("localLoginId" to info.localLoginId),
            )
        }
        val user =
            userRepository.save(
                UserEntity(
                    name = info.name,
                    localLoginId = info.localLoginId,
                    localLoginPasswordHash = BCrypt.hashpw(info.password, BCrypt.gensalt()),
                    userRole = UserRole.CURATOR,
                    snuMail = null,
                ),
            )
        return User.fromEntity(entity = user)
    }

    // Signing in and out

    @Transactional
    fun signIn(request: SignInRequest): Pair<User, UserTokenUtil.Tokens> {
        val user: User =
            when (request.authType) {
                SignInRequest.AuthType.LOCAL -> {
                    val info = request.info as SignInRequest.LocalInfo
                    localSignIn(info)
                }
            }

        // 기존 refresh token 을 만료합니다.(RTR)
        userRedisCacheService.deleteRefreshTokenByUserId(user.id)

        val tokens = UserTokenUtil.generateTokens(user)

        // 발급 받은 refresh token을 redis에 저장합니다.
        userRedisCacheService.saveRefreshToken(user.id, tokens.refreshToken)

        return Pair(user, tokens)
    }

    private fun localSignIn(info: SignInRequest.LocalInfo): User {
        val user =
            userRepository.findByLocalLoginId(info.localLoginId)
                ?: throw InvalidCredentialsException()

        if (!BCrypt.checkpw(info.password, user.localLoginPasswordHash)) {
            throw InvalidCredentialsException()
        }

        return User.fromEntity(entity = user)
    }

    fun signOut(
        user: User,
        refreshToken: String,
    ) {
        val userId =
            userRedisCacheService.getUserIdByRefreshToken(refreshToken)
                ?: throw InvalidRefreshTokenException(
                    details = mapOf("refreshToken" to refreshToken),
                )
        if (user.id != userId) {
            throw TokenMismatchException(
                details = mapOf("userId" to user.id, "refreshTokenUserId" to userId),
            )
        }
        // Additional sign-out logic if necessary

        // 로그아웃 시 Refresh Token 삭제
        // (Access Token 은 클라이언트 측에서 삭제)
        userRedisCacheService.deleteRefreshTokenByUserId(user.id)

        // 추후 유저의 Access Token 을 Access Token 의 남은 유효시간 만큼
        // Redis 블랙리스트에 추가할 필요성 있음
    }

    // Token related functions
    fun authenticate(accessToken: String): User {
        val userId =
            UserTokenUtil.validateAccessTokenGetUserId(accessToken)
                ?: throw InvalidAccessTokenException()

        val user =
            userRepository.findByIdOrNull(userId)
                ?: throw UserNotFoundException(
                    details = mapOf("userId" to userId),
                )
        return User.fromEntity(entity = user)
    }

    @Transactional
    fun refreshAccessToken(refreshToken: String): UserTokenUtil.Tokens {
        val userId =
            userRedisCacheService.getUserIdByRefreshToken(refreshToken)
                ?: throw InvalidRefreshTokenException(
                    details = mapOf("refreshToken" to refreshToken),
                )

        // 기존 refresh token 을 만료합니다.(RTR)
        userRedisCacheService.deleteRefreshTokenByUserId(userId)

        val userEntity =
            userRepository.findByIdOrNull(userId)
                ?: throw UserNotFoundException(
                    details = mapOf("userId" to userId),
                )

        val tokens = UserTokenUtil.generateTokens(User.fromEntity(entity = userEntity))
        // 발급 받은 refresh token을 redis에 저장합니다.
        userRedisCacheService.saveRefreshToken(userEntity.id, tokens.refreshToken)

        return tokens
    }

    // Email verification

    fun fetchGoogleAccountEmail(request: FetchGoogleAccountEmailRequest): String {
        return googleOAuth2Client.getUserInfo(request.accessToken).email
    }

    fun sendSnuMailVerification(request: SendSnuMailVerificationRequest) {
        // 이메일 인증 시 409 에러 불필요
//        if (userRepository.existsBySnuMail(request.snuMail)) {
//            throw UserDuplicateSnuMailException(
//                details = mapOf("snuMail" to request.snuMail),
//            )
//        }

        val emailCode = (100000..999999).random().toString()
        val encryptedEmailCode = BCrypt.hashpw(emailCode, BCrypt.gensalt())

        userRedisCacheService.saveEmailCode(request.snuMail, encryptedEmailCode)
        try {
            emailService.sendEmail(
                to = request.snuMail,
                subject = "[인턴하샤] 이메일 인증 요청 메일이 도착했습니다.",
                text = "이메일 인증 번호: $emailCode",
            )
        } catch (ex: Exception) {
            throw EmailSendFailureException(
                details = mapOf("snuMail" to request.snuMail),
            )
        }
    }

    fun checkSnuMailVerification(request: CheckSnuMailVerificationRequest) {
        val encryptedCode =
            userRedisCacheService.getEmailCode(request.snuMail)
                ?: throw UserEmailVerificationInvalidException(
                    details = mapOf("snuMail" to request.snuMail),
                )

        // 입력된 인증 코드와 Redis에 저장된 암호화된 코드 비교
        if (!BCrypt.checkpw(request.code, encryptedCode)) {
            throw UserEmailVerificationInvalidException(
                details = mapOf("snuMail" to request.snuMail),
            )
        } else {
            userRedisCacheService.deleteEmailCode(request.snuMail)
        }
    }

    @Transactional
    fun withdrawUser(user: User) {
        // 일반 유저가 아닌 경우 탈퇴 불가
        // 추후 curator의 탈퇴도 구현 필요할 수 있음
        // 이 때는 company entity의 author 필드가 null로 변경?
        // @ManyToOne(fetch = FetchType.LAZY, optional = true)
        // @JoinColumn(name = "ADMIN", nullable = true)
        // @OnDelete(action = OnDeleteAction.SET_NULL
        if (user.userRole != UserRole.NORMAL) {
            throw NotAuthorizedException(
                details = mapOf("userId" to user.id, "userRole" to user.userRole),
            )
        }

        val userEntity =
            userRepository.findByIdOrNull(user.id)
                ?: throw UserNotFoundException(
                    details = mapOf("userId" to user.id),
                )

        // 외래키 제약이 걸려있는 bookmark, coffeeChat 를 삭제
        postService.deleteBookmarkByUser(userEntity)
        coffeeChatService.deleteCoffeeChatByUser(userEntity)

        userRepository.deleteUserEntityById(user.id)
        userRedisCacheService.deleteRefreshTokenByUserId(user.id)
    }

    @Transactional
    fun changePassword(
        user: User,
        passwordRequest: ChangePasswordRequest,
    ) {
        val userEntity =
            userRepository.findByIdOrNull(user.id)
                ?: throw UserNotFoundException(
                    details = mapOf("userId" to user.id),
                )

        // 기존 비밀번호를 비교
        if (!BCrypt.checkpw(passwordRequest.oldPassword, userEntity.localLoginPasswordHash)) {
            throw InvalidCredentialsException(
                details = mapOf("oldPassword" to passwordRequest.oldPassword),
            )
        }

        // 새 비밀번호를 저장
        userEntity.localLoginPasswordHash = BCrypt.hashpw(passwordRequest.newPassword, BCrypt.gensalt())
        userRepository.save(userEntity)
    }

    fun findIdAndFetchInfo(findIdRequest: FindIdRequest) {
        // 스누 메일을 기준으로 유저 찾기
        val user =
            userRepository.findBySnuMail(findIdRequest.snuMail)
                ?: throw UserNotFoundException(
                    details = mapOf("snuMail" to findIdRequest.snuMail),
                )

        // 로컬 계정 유저의 정보를 제공 or 소셜 로그인 정보를 제공
        try {
            emailService.sendEmail(
                to = user.snuMail!!,
                subject = "[인턴하샤] 로그인 아이디 정보를 알려드립니다.",
                text = "로그인 아이디 : ${user.localLoginId}",
            )
        } catch (ex: Exception) {
            throw EmailSendFailureException(
                details = mapOf("snuMail" to user.snuMail!!),
            )
        }
    }

    fun resetPassword(resetPasswordRequest: ResetPasswordRequest) {
        // 스누 메일을 기준으로 유저 찾기
        val user =
            userRepository.findBySnuMail(resetPasswordRequest.snuMail)
                ?: throw UserNotFoundException(
                    details = mapOf("snuMail" to resetPasswordRequest.snuMail),
                )

        // 재설정 비밀번호 생성
        val uppercase = ('A'..'Z').random()
        val lowercase = ('a'..'z').random()
        val digit = ('0'..'9').random()
        val specialChars = "@#\$%^&+=!*"
        val special = specialChars.random()

        val allChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val remaining = List(4) { allChars.random() }

        val newPassword =
            (listOf(uppercase, lowercase, digit, special) + remaining)
                .shuffled()
                .joinToString("")

        // 새 비밀번호를 저장
        user.localLoginPasswordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt())
        userRepository.save(user)

        // 로컬 계정 유저의 정보를 제공 or 소셜 로그인 정보를 제공
        try {
            emailService.sendEmail(
                to = user.snuMail!!,
                subject = "[인턴하샤] 임시 비밀번호를 알려드립니다.",
                text =
                    """
                    다음 임시 비밀번호를 이용하여 로그인 후 비밀번호를 재설정하세요.
                    - 임시 비밀번호 : $newPassword
                    """.trimIndent(),
            )
        } catch (ex: Exception) {
            throw EmailSendFailureException(
                details = mapOf("snuMail" to user.snuMail!!),
            )
        }
    }

    // 다른 서비스에서 UserId로 User 가져오기
    fun getUserEntityByUserId(userId: String): UserEntity? = userRepository.findByIdOrNull(userId)

    fun makeDummyUser(index: Int): UserEntity {
        return userRepository.findByLocalLoginId("dummy$index")
            ?: userRepository.save(
                UserEntity(
                    name = "dummy$index",
                    localLoginId = "dummy$index",
                    localLoginPasswordHash = BCrypt.hashpw("DummyPW$index!99", BCrypt.gensalt()),
                    userRole = UserRole.CURATOR,
                    snuMail = null,
                ),
            )
    }

    @Value("\${custom.SECRET}")
    private lateinit var devSecret: String
}
