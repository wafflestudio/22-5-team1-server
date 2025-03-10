package com.waffletoy.team1server.user.utils

import com.waffletoy.team1server.user.dto.User
import io.github.cdimascio.dotenv.Dotenv
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.http.ResponseCookie
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*

object UserTokenUtil {
    data class Tokens(
        val accessToken: String,
        val refreshToken: String,
    )

    // 토큰 생성(access & refresh 쌍)
    // Generate token pair (access & refresh)
    fun generateTokens(user: User): Tokens {
        return Tokens(
            generateAccessToken(user),
            generateRefreshToken(user),
        )
    }

    fun generateAccessToken(user: User): String {
        val now = Instant.now()
        val accessExpiryDate = Date.from(now.plusSeconds(ACCESS_TOKEN_EXPIRATION_TIME))

        return Jwts.builder()
            .signWith(SECRET_KEY)
            .setSubject(user.id) // Use user ID for subject
            .claim("role", user.userRole) // Include role as a custom claim
            .setIssuedAt(Date.from(now))
            .setExpiration(accessExpiryDate)
            .compact()
    }

    fun generateRefreshToken(user: User): String {
        val now = Instant.now()
        val refreshExpiryDate = now.plusSeconds(REFRESH_TOKEN_EXPIRATION_TIME)

        return Jwts.builder()
            .signWith(SECRET_KEY)
            .setSubject(user.id) // Use user ID for subject
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(refreshExpiryDate))
            .compact()
    }

    fun createRefreshTokenCookie(
        token: Tokens,
        isSecure: Boolean = true,
    ): ResponseCookie {
        return ResponseCookie.from("refresh_token", token.refreshToken)
            .httpOnly(true)
            .secure(isSecure) // HTTPS 사용 시 활성화
            .path("/")
            .maxAge(7 * 24 * 60 * 60) // 7일
            .build()
    }

    fun createEmptyRefreshTokenCookie(isSecure: Boolean = true): ResponseCookie {
        return ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .secure(isSecure)
            .path("/")
            .maxAge(0)
            .build()
    }

    // Access Token 유효성 검증
    fun validateAccessTokenGetUserId(token: String): String? {
        return try {
            val claims =
                Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .body
            if (claims.expiration < Date()) {
                null
            } else {
                claims.subject
            }
        } catch (e: Exception) {
            null
        }
    }

    private const val ACCESS_TOKEN_EXPIRATION_TIME = 3600L // 1시간 (초 단위)
    private const val REFRESH_TOKEN_EXPIRATION_TIME = 7 * 24 * 3600L // 7일 (초 단위)
    private const val EMAIL_TOKEN_EXPIRATION_TIME = 180L // 3분 (초 단위)

    var emailTokenExpirationTime: Long = EMAIL_TOKEN_EXPIRATION_TIME
        private set
    var accessTokenExpirationTime: Long = ACCESS_TOKEN_EXPIRATION_TIME
        private set
    var refreshTokenExpirationTime: Long = REFRESH_TOKEN_EXPIRATION_TIME
        private set

    private val dotenv = Dotenv.load()
    private val TOKEN_PRIVATE_KEY =
        dotenv["TOKEN_PRIVATE_KEY"]
            ?: System.getenv("TOKEN_PRIVATE_KEY")
            ?: throw RuntimeException("TOKEN_PRIVATE_KEY not found")
    private val SECRET_KEY =
        runCatching {
            Keys.hmacShaKeyFor(
                TOKEN_PRIVATE_KEY.toByteArray(StandardCharsets.UTF_8),
            )
        }.getOrElse {
            throw IllegalStateException("Invalid TOKEN_PRIVATE_KEY: ${it.message}")
        }
}
