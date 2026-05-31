package com.gymcheck.security.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import com.gymcheck.exception.ApiErrorResponse
import com.gymcheck.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint

class JwtAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper = ObjectMapper(),
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.status = ErrorCode.UNAUTHORIZED.status.value()
        response.contentType = "application/json"
        response.writer.write(
            objectMapper.writeValueAsString(
                ApiErrorResponse(
                    code = ErrorCode.UNAUTHORIZED.code,
                    message = ErrorCode.UNAUTHORIZED.message,
                ),
            ),
        )
    }
}
