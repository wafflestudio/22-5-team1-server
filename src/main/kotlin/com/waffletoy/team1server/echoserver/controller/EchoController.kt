package com.waffletoy.team1server.echoserver.controller

import com.waffletoy.team1server.echoserver.service.EchoService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class EchoController(
    private val echoService: EchoService,
) {
    @Operation(
        summary = "Echo endpoint",
        description = "문자열을 받아 대문자로 변환합니다.",
    )
    @GetMapping("/api/echo/{input}")
    fun echo(
        @Parameter(description = "대문자로 변환할 문자열")
        @PathVariable(required = false) input: String?,
    ): String {
        val value = input ?: "no input"
        return echoService.convertToUpperCase(value)
    }
}