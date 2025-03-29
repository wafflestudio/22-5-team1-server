package com.waffletoy.team1server.applicant

import com.waffletoy.team1server.exceptions.ApiException
import com.waffletoy.team1server.exceptions.ErrorCode

class ApplicantUserForbiddenException(details: Map<String, Any>? = null) : ApiException(
    errorCode = ErrorCode.APPLICANT_USER_FORBIDDEN,
    details = details,
)

class ApplicantNotFoundException(details: Map<String, Any>? = null) : ApiException(
    errorCode = ErrorCode.APPLICANT_NOT_FOUND,
    details = details,
)
