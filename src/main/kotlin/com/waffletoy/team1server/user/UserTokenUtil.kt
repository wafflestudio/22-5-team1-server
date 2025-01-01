package com.waffletoy.team1server.user

import com.waffletoy.team1server.user.persistence.UserEntity
import io.github.cdimascio.dotenv.Dotenv
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*

object UserTokenUtil {
    data class Tokens(
        val accessToken: String,
        val refreshToken: String,
    )

    // 토큰 생성(access & refresh 쌍)
    fun generateTokens(
        userEntity: UserEntity,
    ): Tokens {
        return Tokens(
            generateAccessToken(userEntity),
            generateRefreshToken(userEntity),
        )
    }

    private fun generateAccessToken(userEntity: UserEntity): String {
        val now = Instant.now()
        val accessExpiryDate = Date.from(now.plusSeconds(ACCESS_TOKEN_EXPIRATION_TIME.toLong()))

        return Jwts.builder()
            .signWith(SECRET_KEY)
            .setSubject(userEntity.id)
            .setIssuedAt(Date.from(now))
            .setExpiration(accessExpiryDate)
            .compact()
    }

    private fun generateRefreshToken(userEntity: UserEntity): String {
        val now = Instant.now()
        val refreshExpiryDate = now.plusSeconds(REFRESH_TOKEN_EXPIRATION_TIME)

        return Jwts.builder()
            .signWith(SECRET_KEY)
            .setSubject(userEntity.id)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(refreshExpiryDate))
            .compact()
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
    private const val EMAIL_TOKEN_EXPIRATION_TIME = 3600L // 1시간 (초 단위)

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
        Keys.hmacShaKeyFor(
            TOKEN_PRIVATE_KEY.toByteArray(StandardCharsets.UTF_8),
        )
}
