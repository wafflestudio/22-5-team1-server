package com.waffletoy.team1server.user.utils

import com.waffletoy.team1server.user.dto.Token
import com.waffletoy.team1server.user.dto.User
import com.waffletoy.team1server.user.dto.UserBrief
import com.waffletoy.team1server.user.dto.UserWithAccessToken
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity

object UserTokenResponseUtil {
    fun buildUserWithTokensResponse(
        user: User,
        tokens: UserTokenUtil.Tokens,
        response: HttpServletResponse,
        isSecure: Boolean = true,
    ): ResponseEntity<UserWithAccessToken> {
        val refreshTokenCookie = UserTokenUtil.createRefreshTokenCookie(tokens, isSecure = isSecure)
        response.addHeader("Set-Cookie", refreshTokenCookie.toString())

        return ResponseEntity.ok(
            UserWithAccessToken(
                user =
                    UserBrief(
                        id = user.id,
                        userRole = user.userRole,
                    ),
                token = tokens.accessToken,
            ),
        )
    }

    fun buildTokensResponse(
        tokens: UserTokenUtil.Tokens,
        response: HttpServletResponse,
        isSecure: Boolean = true,
    ): ResponseEntity<Token> {
        val refreshTokenCookie = UserTokenUtil.createRefreshTokenCookie(tokens, isSecure = isSecure)
        response.addHeader("Set-Cookie", refreshTokenCookie.toString())

        return ResponseEntity.ok(
            Token(
                accessToken = tokens.accessToken,
            ),
        )
    }

    fun buildDeleteTokenResponse(
        response: HttpServletResponse,
        isSecure: Boolean = true,
    ): ResponseEntity<Void> {
        val deleteCookie = UserTokenUtil.createEmptyRefreshTokenCookie(isSecure = isSecure)
        response.addHeader("Set-Cookie", deleteCookie.toString())
        return ResponseEntity.ok().build()
    }
}
