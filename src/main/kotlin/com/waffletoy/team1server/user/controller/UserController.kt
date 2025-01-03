package com.waffletoy.team1server.user.controller

import com.waffletoy.team1server.user.AuthProvider
import com.waffletoy.team1server.user.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class UserController(
    private val userService: UserService,
) {
    // 회원가입
    @PostMapping("/signup")
    fun signUp(
        @RequestBody request: SignUpRequest,
    ): ResponseEntity<UserResponse> {
        val user =
            userService.signUp(
                name = request.name,
                email = request.email,
                phoneNumber = request.phoneNumber,
                password = request.password,
                authProvider = request.authProvider,
            )
        return ResponseEntity.ok(
            UserResponse(
                name = user.name,
                email = user.email,
                phoneNumber = user.phoneNumber,
            ),
        )
    }

    // 로그인
    @PostMapping("/signin")
    fun signIn(
        @RequestBody request: SignInRequest,
    ): ResponseEntity<TokensResponse> {
        val (user, tokens) =
            userService.signIn(
                email = request.email,
                password = request.password,
                authProvider = request.authProvider,
            )
        return ResponseEntity.ok(
            TokensResponse(
                userResponse =
                    UserResponse(
                        name = user.name,
                        email = user.email,
                        phoneNumber = user.phoneNumber,
                    ),
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
            ),
        )
    }

    // Access Token 재발급
    @PostMapping("/token/refresh")
    fun refreshAccessToken(
        @RequestBody request: RefreshTokenRequest,
    ): ResponseEntity<TokensResponse> {
        val tokens = userService.refreshAccessToken(request.refreshToken)
        return ResponseEntity.ok(
            TokensResponse(
                userResponse = null,
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
            ),
        )
    }

    // 인증
    @PostMapping("/authenticate")
    fun authenticate(
        @RequestBody request: AccessTokenRequest,
    ): ResponseEntity<UserResponse> {
        val token = request.accessToken.removePrefix("Bearer ")
        val user = userService.authenticate(token)
        return ResponseEntity.ok(
            UserResponse(
                name = user.name,
                email = user.email,
                phoneNumber = user.phoneNumber,
            ),
        )
    }

    // 소셜 로그인을 위한 api
//    @GetMapping("/social-google")
//    fun socialGoogle(): RedirectView {
//        return RedirectView("/oauth2/authorization/google")
//    }
}

data class SignUpRequest(
    val name: String,
    val email: String,
    val phoneNumber: String,
    val password: String?,
    val authProvider: AuthProvider,
)

data class SignInRequest(
    val email: String,
    val password: String,
    val authProvider: AuthProvider,
)

data class RefreshTokenRequest(
    val refreshToken: String,
)

data class AccessTokenRequest(
    val accessToken: String,
)

data class UserResponse(
    val name: String,
    val email: String,
    val phoneNumber: String,
)

data class TokensResponse(
    val userResponse: UserResponse?,
    val accessToken: String,
    val refreshToken: String,
)
