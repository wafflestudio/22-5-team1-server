package com.wafflestudio.internhasha.auth

import com.wafflestudio.internhasha.exceptions.ApiException
import com.wafflestudio.internhasha.exceptions.ErrorCode

class UserDuplicateSnuMailException(details: Map<String, Any>? = null) : ApiException(
    errorCode = ErrorCode.USER_DUPLICATE_SNUMAIL,
    details = details,
)

class UserDuplicateLocalIdException(details: Map<String, Any>? = null) : ApiException(
    errorCode = ErrorCode.USER_DUPLICATE_LOCAL_ID,
    details = details,
)

class UserNotFoundException(details: Map<String, Any>? = null) : ApiException(
    errorCode = ErrorCode.USER_NOT_FOUND,
    details = details,
)

class UserEmailVerificationInvalidException(details: Map<String, Any>? = null) : ApiException(
    errorCode = ErrorCode.USER_EMAIL_VERIFICATION_INVALID,
    details = details,
)

class UserSuccessCodeException(details: Map<String, Any>? = null) : ApiException(
    errorCode = ErrorCode.USER_SUCCESS_CODE_VERIFICATION_FAILED,
    details = details,
)
