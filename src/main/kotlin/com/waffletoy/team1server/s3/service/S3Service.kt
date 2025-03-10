package com.waffletoy.team1server.s3.service

import com.amazonaws.HttpMethod
import com.amazonaws.SdkClientException
import com.amazonaws.services.cloudfront.CloudFrontUrlSigner
import com.amazonaws.services.cloudfront.util.SignerUtils
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.waffletoy.team1server.s3.S3CloudFrontKeyFailedException
import com.waffletoy.team1server.s3.S3FileType
import com.waffletoy.team1server.s3.S3SDKClientFailedException
import com.waffletoy.team1server.s3.S3UrlGenerationFailedException
import com.waffletoy.team1server.s3.controller.S3DownloadReq
import com.waffletoy.team1server.s3.controller.S3UploadReq
import com.waffletoy.team1server.user.dto.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.IOException
import java.security.spec.InvalidKeySpecException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Service
class S3Service(
    @Autowired
    private val amazonS3: AmazonS3,
    @Value("\${amazon.aws.bucketPublic}") private val bucketPublic: String,
    @Value("\${amazon.aws.bucketPrivate}") private val bucketPrivate: String,
    @Value("\${cloudfront.keyPairId}") private val keyPairId: String,
    @Value("\${cloudfront.privateKeyText}") private val privateKeyText: String,
    @Value("\${custom.domain-name}") private val domainName: String,
) {
    // Lazy - 한 번만 파싱하고 이후 재사용하는 구조
    private val tempPrivateKeyFile: File by lazy {
        createPrivateKeyFile()
    }

    private fun createPrivateKeyFile(): File {
        val decodedKey = Base64.getDecoder().decode(privateKeyText) // Base64 디코딩

        return File.createTempFile("temp-private-key", ".pem").apply {
            writeBytes(decodedKey) // 디코딩된 키를 파일로 저장
            deleteOnExit() // 애플리케이션 종료 시 삭제
        }
    }

    // 업로드는 presigned url 사용
    fun generateUploadUrl(
        user: User,
        s3UploadReq: S3UploadReq,
        expirationMinutes: Long = EXPIRATION_MINUTES,
    ): Pair<String, String> {
        val (bucketName, isPrivate) =
            when (s3UploadReq.fileType) {
                S3FileType.CV, S3FileType.PORTFOLIO, S3FileType.IR_DECK, S3FileType.USER_THUMBNAIL -> bucketPrivate to true
                S3FileType.COMPANY_THUMBNAIL -> bucketPublic to false
            }

        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val randomString = UUID.randomUUID().toString().replace("-", "").take(10)
        val filePath = "static/${if (isPrivate) "private" else "public"}/${s3UploadReq.fileType}/${randomString}_$today/${s3UploadReq.fileName}"
        val expiration = calculateExpiration(expirationMinutes)

        return Pair(
            generateS3PresignedUrl(bucketName, filePath, expiration, HttpMethod.PUT),
            filePath,
        )
    }

    // 다운로드는 공개 url & signed url
    fun generateDownloadUrl(
        user: User,
        s3DownloadReq: S3DownloadReq,
        expirationMinutes: Long = EXPIRATION_MINUTES,
    ): String {
        val isPrivate =
            when (s3DownloadReq.fileType) {
                S3FileType.CV, S3FileType.PORTFOLIO, S3FileType.IR_DECK, S3FileType.USER_THUMBNAIL -> true
                S3FileType.COMPANY_THUMBNAIL -> false
            }
        return if (isPrivate) {
            val expiration = calculateExpiration(expirationMinutes)
            generateCloudfrontSignedUrl(s3DownloadReq.filePath, expiration)
        } else {
            "$domainName/${s3DownloadReq.filePath}"
        }
    }

    private fun generateS3PresignedUrl(
        bucketName: String,
        filePath: String,
        expiration: Date,
        httpMethod: HttpMethod,
    ): String {
        try {
            return amazonS3.generatePresignedUrl(bucketName, filePath, expiration, httpMethod).toString()
        } catch (e: AmazonS3Exception) {
            throw S3UrlGenerationFailedException()
        } catch (e: SdkClientException) {
            throw S3SDKClientFailedException()
        }
    }

    private fun generateCloudfrontSignedUrl(
        filePath: String,
        expiration: Date,
    ): String {
        try {
            return CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
                SignerUtils.Protocol.https,
                domainName,
                tempPrivateKeyFile,
                filePath,
                keyPairId,
                expiration,
            )
        } catch (e: Exception) {
            when (e) {
                is InvalidKeySpecException, is IOException -> {
                    throw S3CloudFrontKeyFailedException()
                }
                else -> throw e
            }
        }
    }

    private fun calculateExpiration(expirationMinutes: Long): Date {
        return Date.from(Instant.now().plus(Duration.ofMinutes(expirationMinutes)))
    }

    companion object {
        const val EXPIRATION_MINUTES: Long = 10L
    }
}
