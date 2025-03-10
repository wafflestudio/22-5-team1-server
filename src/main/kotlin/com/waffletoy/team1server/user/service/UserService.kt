package com.waffletoy.team1server.user.service

import com.waffletoy.team1server.coffeeChat.service.CoffeeChatService
import com.waffletoy.team1server.email.EmailSendFailureException
import com.waffletoy.team1server.email.service.EmailService
import com.waffletoy.team1server.exceptions.*
import com.waffletoy.team1server.post.service.PostService
import com.waffletoy.team1server.user.*
import com.waffletoy.team1server.user.controller.*
import com.waffletoy.team1server.user.dto.*
import com.waffletoy.team1server.user.persistence.UserEntity
import com.waffletoy.team1server.user.persistence.UserRepository
import com.waffletoy.team1server.user.utils.PasswordGenerator
import com.waffletoy.team1server.user.utils.UserTokenUtil
import org.mindrot.jbcrypt.BCrypt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Lazy
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userRedisCacheService: UserRedisCacheService,
    @Lazy private val emailService: EmailService,
//    @Lazy private val coffeeChatService: CoffeeChatService,
//    @Lazy private val postService: PostService,
) {
    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Transactional
    fun signUp(request: SignUpRequest): Pair<User, UserTokenUtil.Tokens> {
        val user: User =
            when (request.authType) {
                UserRole.APPLICANT -> {
                    val info = request.info as SignUpRequest.LocalApplicantInfo
                    localApplicantSignUp(info)
                }

                UserRole.COMPANY -> {
                    val info = request.info as SignUpRequest.LocalCompanyInfo
                    localCompanySignUp(info)
                }
            }
        val tokens = UserTokenUtil.generateTokens(user)

        // 발급 받은 refresh token을 redis에 저장합니다.
        userRedisCacheService.saveRefreshToken(user.id, tokens.refreshToken)
        return Pair(user, tokens)
    }

    private fun localApplicantSignUp(info: SignUpRequest.LocalApplicantInfo): User {
        // 이미 존재하는 계정 확인
        val existingUser = userRepository.findByMail(info.mail)
        existingUser?.let {
            if (it.userRole != UserRole.APPLICANT) {
                throw NotAuthorizedException(
                    details = mapOf("userId" to it.id, "userRole" to it.userRole),
                )
            }
            throw UserDuplicateSnuMailException(
                details = mapOf("snuMail" to info.mail),
            )
        }

        // 이메일(아이디) 중복 확인
        if (userRepository.existsByMail(info.mail)) {
            throw UserDuplicateLocalIdException(
                details = mapOf("mail" to info.mail),
            )
        }

        // 새로운 사용자 생성
        val user =
            UserEntity(
                name = info.name,
                mail = info.mail,
                passwordHash = BCrypt.hashpw(info.password, BCrypt.gensalt()),
                userRole = UserRole.APPLICANT,
            ).let { userRepository.save(it) }

        return User.fromEntity(user)
    }

    private fun localCompanySignUp(info: SignUpRequest.LocalCompanyInfo): User {
        // 관리자 비밀번호 확인
        if (info.secretPassword != devSecret) {
            throw InvalidCredentialsException(
                details = mapOf("secretPassword" to info.secretPassword),
            )
        }

        // 이메일(아이디) 중복 확인
        if (userRepository.existsByMail(info.mail)) {
            throw UserDuplicateLocalIdException(
                details = mapOf("mail" to info.mail),
            )
        }

        // 새로운 사용자 생성
        val user =
            UserEntity(
                name = info.name,
                mail = info.mail,
                passwordHash = BCrypt.hashpw(info.password, BCrypt.gensalt()),
                userRole = UserRole.COMPANY,
            ).let { userRepository.save(it) }

        return User.fromEntity(user)
    }

    // 로그인
    @Transactional
    fun signIn(request: SignInRequest): Pair<User, UserTokenUtil.Tokens> {
        val userEntity =
            userRepository.findByMail(request.mail)
                ?: throw InvalidCredentialsException()

        if (!BCrypt.checkpw(request.password, userEntity.passwordHash)) {
            throw InvalidCredentialsException()
        }

        val user = User.fromEntity(userEntity)

        // 기존 refresh token 을 만료합니다.(RTR)
        userRedisCacheService.deleteRefreshTokenByUserId(user.id)

        val tokens = UserTokenUtil.generateTokens(user)

        // 발급 받은 refresh token을 redis에 저장합니다.
        userRedisCacheService.saveRefreshToken(user.id, tokens.refreshToken)

        return Pair(user, tokens)
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

        // 로그아웃 시 Refresh Token 삭제
        // (Access Token 은 클라이언트 측에서 삭제)
        userRedisCacheService.deleteRefreshTokenByUserId(user.id)

        // 추후 유저의 Access Token 을 Access Token 의 남은 유효시간 만큼
        // Redis 블랙리스트에 추가할 필요성 있음
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

    // 메일(아이디) 중복 확인
    @Transactional(readOnly = true)
    fun checkDuplicateMail(request: MailRequest) {
        if (userRepository.existsByMail(request.mail)) {
            throw UserDuplicateSnuMailException(
                details = mapOf("mail" to request.mail),
            )
        }
    }

    // Email verification
    fun sendSnuMailVerification(request: SnuMailRequest) {
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
        if (user.userRole != UserRole.APPLICANT) {
            throw NotAuthorizedException(
                details = mapOf("userId" to user.id, "userRole" to user.userRole),
            )
        }

        val userEntity =
            userRepository.findByIdOrNull(user.id)
                ?: throw UserNotFoundException(
                    details = mapOf("userId" to user.id),
                )

        // 순환 의존성 해결: 동적으로 빈을 가져와서 사용
        val postService = applicationContext.getBean(PostService::class.java)
        val coffeeChatService = applicationContext.getBean(CoffeeChatService::class.java)

        // 외래키 제약이 걸려있는 bookmark, coffeeChat 삭제
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
        if (!BCrypt.checkpw(passwordRequest.oldPassword, userEntity.passwordHash)) {
            throw InvalidCredentialsException(
                details = mapOf("oldPassword" to passwordRequest.oldPassword),
            )
        }

        // 새 비밀번호를 저장
        userEntity.passwordHash = BCrypt.hashpw(passwordRequest.newPassword, BCrypt.gensalt())
        userRepository.save(userEntity)
    }

    @Transactional
    fun resetPassword(mailRequest: MailRequest) {
        // 메일을 기준으로 유저 찾기
        val user =
            userRepository.findByMail(mailRequest.mail)
                ?: throw UserNotFoundException(
                    details = mapOf("mail" to mailRequest.mail),
                )

        val newPassword = PasswordGenerator.generateRandomPassword()

        // 새 비밀번호를 저장
        user.passwordHash = BCrypt.hashpw(newPassword, BCrypt.gensalt())
        userRepository.save(user)

        // 로컬 계정 유저의 정보를 제공 or 소셜 로그인 정보를 제공
        try {
            emailService.sendEmail(
                to = user.mail,
                subject = "[인턴하샤] 임시 비밀번호를 알려드립니다.",
                text =
                    """
                    다음 임시 비밀번호를 이용하여 로그인 후 비밀번호를 재설정하세요.
                    - 임시 비밀번호 : $newPassword
                    """.trimIndent(),
            )
        } catch (ex: Exception) {
            throw EmailSendFailureException(
                details = mapOf("mail" to user.mail),
            )
        }
    }

    // 다른 서비스에서 UserId로 User 가져오기
    @Transactional(readOnly = true)
    fun getUserEntityByUserId(userId: String): UserEntity? = userRepository.findByIdOrNull(userId)

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

    fun makeDummyUser(index: Int): UserEntity {
        return userRepository.findByMail("dummy$index@gmail.com")
            ?: userRepository.save(
                UserEntity(
                    name = "dummy$index",
                    mail = "dummy$index@gmail.com",
                    passwordHash = BCrypt.hashpw("DummyPW$index!99", BCrypt.gensalt()),
                    userRole = UserRole.COMPANY,
                ),
            )
    }

    @Value("\${custom.SECRET}")
    private lateinit var devSecret: String
}
