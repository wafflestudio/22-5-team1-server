package com.waffletoy.team1server.echoserver.service

import org.springframework.stereotype.Service

@Service
class EchoService {
    fun convertToUpperCase(input: String): String {
        return input.uppercase()
    }
}
