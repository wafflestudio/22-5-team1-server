package com.waffletoy.team1server.user.utils

import java.security.SecureRandom
import kotlin.random.asKotlinRandom

object PasswordGenerator {
    private val upper = ('A'..'Z')
    private val lower = ('a'..'z')
    private val digits = ('0'..'9')
    private val specialChars = listOf('@', '#', '$', '%', '^', '&', '+', '=', '!', '*') // ✅ 수정

    private val allChars = upper + lower + digits + specialChars
    private val random = SecureRandom().asKotlinRandom()

    fun generateRandomPassword(length: Int = 10): String {
        return (
            listOf(
                upper.random(random),
                lower.random(random),
                digits.random(random),
                specialChars.random(random),
            ) + List(length - 4) { allChars.random(random) }
        )
            .shuffled()
            .joinToString("")
    }
}
