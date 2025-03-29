package com.waffletoy.team1server.coffeeChat.controller

import com.waffletoy.team1server.auth.AuthUser
import com.waffletoy.team1server.auth.UserRole
import com.waffletoy.team1server.auth.dto.User
import com.waffletoy.team1server.coffeeChat.CoffeeChatStatus
import com.waffletoy.team1server.coffeeChat.dto.CoffeeChatApplicant
import com.waffletoy.team1server.coffeeChat.dto.CoffeeChatBrief
import com.waffletoy.team1server.coffeeChat.dto.CoffeeChatDetail
import com.waffletoy.team1server.coffeeChat.service.CoffeeChatService
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/coffeeChat")
@Validated
class CoffeeChatController(
    private val coffeeChatService: CoffeeChatService,
) {
    // 커피챗 상세 페이지 불러오기
    @GetMapping("/{coffeeChatId}")
    fun getCoffeeChatDetail(
        @Parameter(hidden = true) @AuthUser user: User,
        @PathVariable coffeeChatId: String,
    ): ResponseEntity<CoffeeChatDetail> {
        return ResponseEntity.ok(
            coffeeChatService.getCoffeeChatDetail(user, coffeeChatId),
        )
    }

    // 지원자 - 이미 대기 중 커피챗 있는지 확인
    @GetMapping("/{postId}/status")
    fun checkIsSubmitted(
        @Parameter(hidden = true) @AuthUser user: User,
        @PathVariable postId: String,
    ): ResponseEntity<CoffeeChatIsSubmitted> {
        return ResponseEntity.ok(
            CoffeeChatIsSubmitted(
                coffeeChatService.checkIsSubmitted(user, postId),
            ),
        )
    }

    // 지원자 - 커피챗 신청하기
    @PostMapping("/{postId}")
    fun postCoffeeChat(
        @Parameter(hidden = true) @AuthUser user: User,
        @PathVariable postId: String,
        @RequestBody coffeeChatContent: CoffeeChatContent,
    ): ResponseEntity<CoffeeChatApplicant> {
        return ResponseEntity.ok(
            coffeeChatService.applyCoffeeChat(
                user,
                postId,
                coffeeChatContent,
            ),
        )
    }

    // 지원자 - 커피챗 수정하기
    @PutMapping("/{coffeeChatId}")
    fun editCoffeeChat(
        @Parameter(hidden = true) @AuthUser user: User,
        @PathVariable coffeeChatId: String,
        @RequestBody coffeeChatContent: CoffeeChatContent,
    ): ResponseEntity<CoffeeChatApplicant> {
        return ResponseEntity.ok(
            coffeeChatService.editCoffeeChat(
                user,
                coffeeChatId,
                coffeeChatContent,
            ),
        )
    }

    // 커피챗 상태 변경
    @PatchMapping
    fun changeCoffeeChatStatus(
        @Parameter(hidden = true) @AuthUser user: User,
        @Valid @RequestBody coffeeChatStatusReq: CoffeeChatStatusReq,
    ): ResponseEntity<CoffeeChatDetailList> {
        return ResponseEntity.ok(
            coffeeChatService.changeCoffeeChatStatus(
                user,
                coffeeChatStatusReq,
            ),
        )
    }

    // 커피챗 목록 불러오기
    @GetMapping
    fun getCoffeeChatList(
        @Parameter(hidden = true) @AuthUser user: User,
    ): ResponseEntity<CoffeeChatList> {
        // Transactional을 위해 호출 분리
        val coffeeChatList =
            when (user.userRole) {
                UserRole.APPLICANT -> coffeeChatService.getCoffeeChatListApplicant(user)
                UserRole.COMPANY -> coffeeChatService.getCoffeeChatListCompany(user)
            }
        return ResponseEntity.ok(
            CoffeeChatList(
                coffeeChatList = coffeeChatList,
            ),
        )
    }

    // 배지 표시 커피챗 개수
    @GetMapping("/count")
    fun countCoffeeChatBadges(
        @Parameter(hidden = true) @AuthUser user: User,
    ): ResponseEntity<CoffeeChatCount> {
        return ResponseEntity.ok(
            CoffeeChatCount(
                num = coffeeChatService.countCoffeeChatBadges(user),
            ),
        )
    }
}

data class CoffeeChatContent(
    @field:NotBlank(message = "Content cannot be blank.")
    @field:Size(min = 1, max = 10000, message = "Content must be at most 10,000 characters long")
    val content: String,
)

data class CoffeeChatStatusReq(
    @field:NotNull(message = "Status cannot be null.")
    val coffeeChatStatus: CoffeeChatStatus,
    @field:NotEmpty(message = "List cannot not empty")
    val coffeeChatList: List<String>,
)

data class CoffeeChatList(
    val coffeeChatList: List<CoffeeChatBrief>,
)

data class CoffeeChatCount(
    val num: Int,
)

data class CoffeeChatIsSubmitted(
    val isSubmitted: Boolean,
)

data class CoffeeChatDetailList(
    val succeeded: List<CoffeeChatDetail>,
    val failed: List<CoffeeChatDetail>,
)
