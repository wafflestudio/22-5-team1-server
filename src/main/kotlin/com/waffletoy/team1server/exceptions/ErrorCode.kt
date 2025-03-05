package com.waffletoy.team1server.exceptions

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val code: String,
    val description: String,
    val httpStatus: HttpStatus,
) {
    // User-related errors
    USER_DUPLICATE_SNUMAIL("USER_001", "A user with the provided SNU mail already exists.", HttpStatus.CONFLICT),
    USER_DUPLICATE_LOCAL_ID("USER_002", "A user with the provided local login ID already exists.", HttpStatus.CONFLICT),
    USER_NOT_FOUND("USER_004", "User not found.", HttpStatus.NOT_FOUND),
    USER_INVALID_ROLE("USER_005", "Invalid user role.", HttpStatus.FORBIDDEN),
    USER_EMAIL_VERIFICATION_INVALID("USER_006", "Invalid email verification code.", HttpStatus.BAD_REQUEST),

    // Authentication-related errors
    AUTH_BAD_HEADER("AUTH_001", "Wrong Authorization header", HttpStatus.BAD_REQUEST),
    AUTH_INVALID_CREDENTIALS("AUTH_002", "Invalid credentials provided.", HttpStatus.UNAUTHORIZED),
    AUTH_INVALID_REFRESH_TOKEN("AUTH_003", "Invalid or expired refresh token.", HttpStatus.UNAUTHORIZED),
    AUTH_INVALID_ACCESS_TOKEN("AUTH_004", "Invalid access token.", HttpStatus.UNAUTHORIZED),
    AUTH_TOKEN_MISMATCH("AUTH_005", "Access token does not match the refresh token.", HttpStatus.BAD_REQUEST),
    AUTH_OAUTH_FAILURE("AUTH_006", "OAUTH failure.", HttpStatus.UNAUTHORIZED),
    AUTH_NOT_AUTHORIZED("AUTH_007", "Not authorized.", HttpStatus.UNAUTHORIZED),

    // Email-related errors
    EMAIL_SEND_FAILURE("EMAIL_001", "Failed to send verification email.", HttpStatus.INTERNAL_SERVER_ERROR),

    // General errors
    INTERNAL_SERVER_ERROR("GEN_001", "An unexpected error occurred.", HttpStatus.INTERNAL_SERVER_ERROR),
    NOT_IMPLEMENTED("GEN_002", "This functionality is not yet implemented.", HttpStatus.NOT_IMPLEMENTED),
    INVALID_REQUEST("GEN_003", "Invalid request parameters.", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED("GEN_004", "Validation failed for the request.", HttpStatus.BAD_REQUEST),
    INVALID_JSON("GEN_005", "Malformed JSON request.", HttpStatus.BAD_REQUEST),

    // Post-related errors
    POST_NOT_FOUND("POST_001", "The requested post was not found.", HttpStatus.NOT_FOUND),
    POST_ALREADY_BOOKMARKED("POST_002", "The post is already bookmarked.", HttpStatus.CONFLICT),
    POST_BOOKMARK_NOT_FOUND("POST_003", "The bookmark for the post does not exist.", HttpStatus.NOT_FOUND),
    POST_INVALID_FILTERS("POST_004", "Invalid filters provided for fetching posts.", HttpStatus.BAD_REQUEST),
    POST_CREATION_FAILED("POST_005", "Failed to create the post.", HttpStatus.INTERNAL_SERVER_ERROR),
    POST_DELETION_FAILED("POST_006", "Failed to delete the post.", HttpStatus.INTERNAL_SERVER_ERROR),
    POST_COMPANY_EXISTS("POST_007", "Company email already exists", HttpStatus.CONFLICT),
    POST_COMPANY_NOT_FOUND("POST_008", "Company not found.", HttpStatus.NOT_FOUND),
    POST_POSITION_NOT_FOUND("POST_009", "Position not found", HttpStatus.NOT_FOUND),
    POST_S3_URL_GENERATION_FAILED("POST_010", "S3 URL generation failed.", HttpStatus.INTERNAL_SERVER_ERROR),
    POST_S3_SDK_FAILED("POST_011", "AWS SKD Client error", HttpStatus.INTERNAL_SERVER_ERROR),

    // COFFEECHAT-related errors
    COFFEECHAT_NOT_FOUND("COFFEECHAT_001", "The requested coffeeChat was not found.", HttpStatus.NOT_FOUND),
    COFFEECHAT_USER_FORBIDDEN("COFFEECHAT_002", "You do not have permission to perform this action.", HttpStatus.FORBIDDEN),
    COFFEECHAT_CREATION_FAILED("COFFEECHAT_003", "Failed to create the coffeeChat.", HttpStatus.INTERNAL_SERVER_ERROR),
    COFFEECHAT_DELETION_FAILED("COFFEECHAT_004", "Failed to delete the coffeeChat.", HttpStatus.INTERNAL_SERVER_ERROR),
    COFFEECHAT_UPDATE_FAILED("COFFEECHAT_005", "Failed to update the coffeeChat.", HttpStatus.INTERNAL_SERVER_ERROR),
    COFFEECHAT_INVALID_USER_ROLE("COFFEECHAT_006", "Invalid user role for coffeeChat operations.", HttpStatus.FORBIDDEN),
    COFFEECHAT_POST_EXPIRED("COFFEECHAT_007", "The application for Coffee Chat is now closed.(Post deadline)", HttpStatus.FORBIDDEN),
    COFFEECHAT_STATUS_FORBIDDEN("COFFEECHAT_008", "You cannot change the status of the coffeeChat", HttpStatus.FORBIDDEN),
}
