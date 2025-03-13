package com.waffletoy.team1server.s3.controller

import com.waffletoy.team1server.auth.AuthUser
import com.waffletoy.team1server.auth.dto.User
import com.waffletoy.team1server.s3.S3FileType
import com.waffletoy.team1server.s3.service.S3Service
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/s3")
@Validated
class S3Controller(
    private val s3Service: S3Service,
) {
    @PostMapping
    fun generateUploadUrl(
        @Parameter(hidden = true) @AuthUser user: User,
        @Valid @RequestBody s3UploadReq: S3UploadReq,
    ): ResponseEntity<S3UploadResp> {
        val (url, s3Key) = s3Service.generateUploadUrl(user, s3UploadReq)
        return ResponseEntity.ok(S3UploadResp(url, s3Key))
    }

    @GetMapping
    fun generateDownloadUrl(
        @Parameter(hidden = true) @AuthUser user: User,
        @RequestParam(required = true) s3Key: String,
        @RequestParam(required = true) fileType: S3FileType,
    ): ResponseEntity<S3DownloadResp> {
        val s3DownloadReq = S3DownloadReq(fileType, s3Key)
        val url = s3Service.generateDownloadUrl(user, s3DownloadReq)
        return ResponseEntity.ok(S3DownloadResp(url))
    }
}

data class S3UploadReq(
    @field:NotBlank(message = "파일 이름은 필수입니다.")
    val fileName: String,
    @field:NotNull(message = "파일 타입은 필수입니다.")
    val fileType: S3FileType,
)

data class S3UploadResp(
    val url: String,
    val s3Key: String,
)

data class S3DownloadReq(
    @field:NotNull(message = "파일 타입은 필수입니다.")
    val fileType: S3FileType,
    @field:NotBlank(message = "파일 경로는 필수입니다.")
    val s3Key: String,
)

data class S3DownloadResp(
    val url: String,
)
