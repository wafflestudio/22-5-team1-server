package com.waffletoy.team1server.coffeeChat.controller

import com.waffletoy.team1server.coffeeChat.dto.CoffeeChatApplicant
import com.waffletoy.team1server.coffeeChat.dto.CoffeeChatBrief
import com.waffletoy.team1server.coffeeChat.dto.CoffeeChatCompany
import com.waffletoy.team1server.coffeeChat.service.CoffeeChatService
import com.waffletoy.team1server.user.AuthUser
import com.waffletoy.team1server.user.dtos.User
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.constraints.NotBlank
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/coffeeChat")
@Validated
class CoffeeChatController(
    private val coffeeChatService: CoffeeChatService,
) {
    // 지원자 계정 - 커피챗 상세 페이지 불러오기
    @GetMapping("/{coffeeChatId}/applicant")
    fun getCoffeeChatDetailApplicant(
        @Parameter(hidden = true) @AuthUser user: User,
        @PathVariable coffeeChatId: String,
    ): ResponseEntity<CoffeeChatApplicant> {
        return ResponseEntity.ok(
            coffeeChatService.getCoffeeChatDetailApplicant(user, coffeeChatId),
        )
    }

    // 회사 계정 - 커피챗 상세 페이지 불러오기
    @GetMapping("/{coffeeChatId}/company")
    fun getCoffeeChatDetailCompany(
        @Parameter(hidden = true) @AuthUser user: User,
        @PathVariable coffeeChatId: String,
    ): ResponseEntity<CoffeeChatCompany> {
        return ResponseEntity.ok(
            coffeeChatService.getCoffeeChatDetailCompany(user, coffeeChatId),
        )
    }

    // 지원자 - 커피챗 신청하기
    @PostMapping("/{postId}/applicant/apply")
    fun postCoffeeChat(
        @Parameter(hidden = true) @AuthUser user: User,
        @PathVariable postId: String,
        @RequestBody coffeeChatRequest: CoffeeChatRequest,
    ): ResponseEntity<CoffeeChatApplicant> {
        val coffeeChat =
            coffeeChatService.applyCoffeeChat(
                user,
                postId,
                coffeeChatRequest,
            )
        return ResponseEntity.ok(coffeeChat)
    }

    // 지원자 - 커피챗 수정하기
    @PatchMapping("/{coffeeChatId}/applicant/edit")
    fun editCoffeeChat(
        @Parameter(hidden = true) @AuthUser user: User,
        @PathVariable coffeeChatId: String,
        @RequestBody coffeeChatRequest: CoffeeChatRequest,
    ): ResponseEntity<CoffeeChatApplicant> {
        val updatedCoffeeChat = coffeeChatService.editCoffeeChat(user, coffeeChatId, coffeeChatRequest)
        return ResponseEntity.ok(updatedCoffeeChat)
    }

    // 지원자 - 커피챗 취소하기
    @PatchMapping("/{coffeeChatId}/applicant/cancel")
    fun cancelCoffeeChat(
        @Parameter(hidden = true) @AuthUser user: User,
        @PathVariable coffeeChatId: String,
    ): ResponseEntity<CoffeeChatApplicant> {
        val updatedCoffeeChat = coffeeChatService.cancelCoffeeChat(user, coffeeChatId)
        return ResponseEntity.ok(updatedCoffeeChat)
    }

    // 회사 - 커피챗 수락하기
    @PatchMapping("/{coffeeChatId}/company/confirm")
    fun confirmCoffeeChat(
        @Parameter(hidden = true) @AuthUser user: User,
        @PathVariable coffeeChatId: String,
    ): ResponseEntity<CoffeeChatCompany> {
        val updatedCoffeeChat = coffeeChatService.confirmCoffeeChat(user, coffeeChatId)
        return ResponseEntity.ok(updatedCoffeeChat)
    }

    // 회사 - 커피챗 거절하기
    @PatchMapping("/{coffeeChatId}/company/reject")
    fun rejectCoffeeChat(
        @Parameter(hidden = true) @AuthUser user: User,
        @PathVariable coffeeChatId: String,
    ): ResponseEntity<CoffeeChatCompany> {
        val updatedCoffeeChat = coffeeChatService.rejectCoffeeChat(user, coffeeChatId)
        return ResponseEntity.ok(updatedCoffeeChat)
    }

    // 지원자 - 커피챗 목록 불러오기
    @GetMapping("/applicant")
    fun getCoffeeChatListApplicant(
        @Parameter(hidden = true) @AuthUser user: User,
    ): ResponseEntity<CoffeeChatList> {
        return ResponseEntity.ok(
            CoffeeChatList(
                coffeeChatList = coffeeChatService.getCoffeeChatListApplicant(user),
            ),
        )
    }

    // 회사 - 커피챗 목록 불러오기
    @GetMapping("/company")
    fun getCoffeeChatListCompany(
        @Parameter(hidden = true) @AuthUser user: User,
    ): ResponseEntity<CoffeeChatList> {
        return ResponseEntity.ok(
            CoffeeChatList(
                coffeeChatList = coffeeChatService.getCoffeeChatListCompany(user),
            ),
        )
    }

    // 지원자 - 새로 업데이트된 커피챗 개수 가져오기
    @GetMapping("/changed")
    fun countChangedCoffeeChatApplicant(
        @Parameter(hidden = true) @AuthUser user: User,
    ): ResponseEntity<CoffeeChatChanged> {
        return ResponseEntity.ok(
            CoffeeChatChanged(
                numChanged = coffeeChatService.countChangedCoffeeChatApplicant(user),
            ),
        )
    }

    // 지원자 - 새로 업데이트된 커피챗 개수 가져오기
    @GetMapping("/waiting")
    fun countWaitingCoffeeChatsCompany(
        @Parameter(hidden = true) @AuthUser user: User,
    ): ResponseEntity<CoffeeChatWaiting> {
        return ResponseEntity.ok(
            CoffeeChatWaiting(
                numWaiting = coffeeChatService.countWaitingCoffeeChatsCompany(user),
            ),
        )
    }
}

data class CoffeeChatRequest(
    @field:NotBlank(message = "Content cannot be blank.")
    val content: String,
)

data class CoffeeChatList(
    val coffeeChatList: List<CoffeeChatBrief>,
)

data class CoffeeChatChanged(
    val numChanged: Int,
)

data class CoffeeChatWaiting(
    val numWaiting: Int,
)
