package com.waffletoy.team1server.user.controller

import com.waffletoy.team1server.user.AuthUser
import com.waffletoy.team1server.user.dto.*
import com.waffletoy.team1server.user.service.UserService
import com.waffletoy.team1server.user.utils.UserTokenResponseUtil
import io.swagger.v3.oas.annotations.Parameter
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService,
    @Value("\${custom.is-secure}") private val isSecure: Boolean,
) {
    // 회원가입
    @PostMapping
    fun signUp(
        @Valid @RequestBody request: SignUpRequest,
        response: HttpServletResponse,
    ): ResponseEntity<UserWithAccessToken> {
        val (user, tokens) = userService.signUp(request)
        return UserTokenResponseUtil.buildUserWithTokensResponse(user, tokens, response, isSecure)
    }

    // 로그인
    @PostMapping("/auth")
    fun signIn(
        @Valid @RequestBody request: SignInRequest,
        response: HttpServletResponse,
    ): ResponseEntity<UserWithAccessToken> {
        val (user, tokens) = userService.signIn(request)
        return UserTokenResponseUtil.buildUserWithTokensResponse(user, tokens, response, isSecure)
    }

    // 로그아웃
    @DeleteMapping("/auth")
    fun signOut(
        @Parameter(hidden = true) @AuthUser user: User,
        @CookieValue("refresh_token") refreshToken: String?,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        if (refreshToken.isNullOrBlank()) {
            return ResponseEntity.badRequest().build()
        }

        userService.signOut(user, refreshToken)
        return UserTokenResponseUtil.buildDeleteTokenResponse(response)
    }

    // refresh token을 이용한 토큰 갱신
    @GetMapping("/token")
    fun refreshAccessToken(
        @CookieValue("refresh_token") refreshToken: String?,
        response: HttpServletResponse,
    ): ResponseEntity<Token> {
        if (refreshToken.isNullOrBlank()) {
            return ResponseEntity.badRequest().build()
        }
        val tokens = userService.refreshAccessToken(refreshToken)
        return UserTokenResponseUtil.buildTokensResponse(tokens, response)
    }

    // 메일 중복 확인
    @PostMapping("/mail")
    fun checkDuplicateMail(
        @Valid @RequestBody request: MailRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        userService.checkDuplicateMail(request)
        return ResponseEntity.ok().build()
    }

    // 스누메일 인증을 위한 코드 발송
    @PostMapping("/snu-mail/verification")
    fun sendSnuMailVerification(
        @Valid @RequestBody request: SnuMailRequest,
    ): ResponseEntity<Void> {
        userService.sendSnuMailVerification(request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/snu-mail-verification/verify")
    fun checkSnuMailVerification(
        @Valid @RequestBody request: CheckSnuMailVerificationRequest,
    ): ResponseEntity<Void> {
        userService.checkSnuMailVerification(request)
        return ResponseEntity.ok().build()
    }

    // 회원 탈퇴
    @DeleteMapping
    fun deleteUser(
        @Parameter(hidden = true) @AuthUser user: User,
    ): ResponseEntity<Void> {
        userService.withdrawUser(user)
        return ResponseEntity.ok().build()
    }

    // 비밀 번호 변경
    @PatchMapping("/password")
    fun changePassword(
        @Parameter(hidden = true) @AuthUser user: User,
        @Valid @RequestBody request: ChangePasswordRequest,
    ): ResponseEntity<Void> {
        userService.changePassword(user, request)
        return ResponseEntity.ok().build()
    }

    // 임시 비밀번호를 스누메일로 전송
    @PostMapping("/password")
    fun resetPassword(
        @Valid @RequestBody request: MailRequest,
    ): ResponseEntity<Void> {
        userService.resetPassword(request)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/me")
    fun getUserInfo(
        @Parameter(hidden = true) @AuthUser user: User,
    ): ResponseEntity<User> {
        return ResponseEntity.ok(user)
    }
}

data class CheckSnuMailVerificationRequest(
    @field:NotBlank(message = "Email is required")
    @field:Pattern(regexp = "^[a-zA-Z0-9._%+-]+@snu\\.ac\\.kr$", message = "Email must end with @snu.ac.kr")
    val snuMail: String,
    @field:NotBlank(message = "Verification code is required")
    val code: String,
)

data class SnuMailRequest(
    @field:NotBlank(message = "Email is required")
    @field:Pattern(regexp = "^[a-zA-Z0-9._%+-]+@snu\\.ac\\.kr$", message = "Email must end with @snu.ac.kr")
    val snuMail: String,
)

data class MailRequest(
    @field:NotBlank(message = "email is required")
    @field:Email(message = "email is required")
    val mail: String,
)

data class ChangePasswordRequest(
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!*]).{8,20}$",
        message = "Password must contain at least 1 uppercase letter, 1 lowercase letter, 1 number, and 1 special character.",
    )
    val oldPassword: String,
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#\$%^&+=!*]).{8,20}$",
        message = "Password must contain at least 1 uppercase letter, 1 lowercase letter, 1 number, and 1 special character.",
    )
    val newPassword: String,
)
