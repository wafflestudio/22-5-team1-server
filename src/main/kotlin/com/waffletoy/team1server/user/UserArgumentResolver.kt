package com.waffletoy.team1server.user

import com.waffletoy.team1server.exceptions.ApiException
import com.waffletoy.team1server.exceptions.BadAuthorizationHeaderException
import com.waffletoy.team1server.exceptions.InvalidAccessTokenException
import com.waffletoy.team1server.user.dto.User
import com.waffletoy.team1server.user.service.UserService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class UserArgumentResolver(
    private val userService: UserService,
) : HandlerMethodArgumentResolver {
    private val logger: Logger = LoggerFactory.getLogger(UserArgumentResolver::class.java)

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val isSupported =
            parameter.parameterType == User::class.java &&
                parameter.hasParameterAnnotation(AuthUser::class.java)
        logger.debug("supportsParameter called for parameter '${parameter.parameterName}': $isSupported")
        return isSupported
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): User {
        logger.debug("resolveArgument called for parameter: ${parameter.parameterName}")

        val authorizationHeader = webRequest.getHeader("Authorization")
        logger.debug("Authorization Header: $authorizationHeader")

        // Check if the Authorization header is present and not blank
        if (authorizationHeader.isNullOrBlank()) {
            logger.warn("Authorization header is missing or blank")
            throw BadAuthorizationHeaderException(
                details = mapOf("Authorization" to "Missing or invalid Authorization header"),
            )
        }

        // Split the header and validate the format (e.g., "Bearer <token>")
        val tokenParts = authorizationHeader.split(" ")
        if (tokenParts.size != 2 || tokenParts[0] != "Bearer") {
            logger.warn("Authorization header is malformed: $authorizationHeader")
            throw BadAuthorizationHeaderException(
                details = mapOf("Authorization" to "Malformed Authorization header. Expected format: Bearer <token>"),
            )
        }

        val accessToken = tokenParts[1]
        logger.debug("Extracted Access Token: $accessToken")

        return try {
            // Authenticate the user using the extracted access token
            userService.authenticate(accessToken).also {
                logger.debug("Authenticated User: $it")
            }
        } catch (ex: ApiException) {
            // Propagate ApiException without modification
            logger.error("Authentication failed: ${ex.message}", ex)
            throw ex
        } catch (ex: Exception) {
            // Handle unexpected exceptions by wrapping them in an ApiException
            logger.error("Unexpected error during authentication: ${ex.message}", ex)
            throw InvalidAccessTokenException(
                details = mapOf("error" to ex.message.orEmpty()),
            )
        }
    }
}
