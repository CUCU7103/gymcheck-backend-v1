package com.gymcheck.exception

import java.time.DateTimeException
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(DateTimeException::class)
    fun handleDateTimeException(exception: DateTimeException): ResponseEntity<ApiErrorResponse> {
        return ResponseEntity
            .status(ErrorCode.BAD_REQUEST.status)
            .body(ApiErrorResponse(ErrorCode.BAD_REQUEST.code, "올바르지 않은 날짜 형식입니다."))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(exception: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
        val message = exception.bindingResult.fieldErrors
            .joinToString(separator = ", ") { error: FieldError ->
                "${error.field}: ${error.defaultMessage ?: "invalid value"}"
            }
            .ifBlank { ErrorCode.BAD_REQUEST.message }

        return ResponseEntity
            .status(ErrorCode.BAD_REQUEST.status)
            .body(ApiErrorResponse(ErrorCode.BAD_REQUEST.code, message))
    }

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(exception: CustomException): ResponseEntity<ApiErrorResponse> {
        return ResponseEntity
            .status(exception.errorCode.status)
            .body(
                ApiErrorResponse(
                    code = exception.errorCode.code,
                    message = exception.message ?: exception.errorCode.message,
                ),
            )
    }

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<ApiErrorResponse> {
        log.error("Unhandled exception", exception)

        return ResponseEntity
            .status(ErrorCode.INTERNAL_SERVER_ERROR.status)
            .body(
                ApiErrorResponse(
                    code = ErrorCode.INTERNAL_SERVER_ERROR.code,
                    message = ErrorCode.INTERNAL_SERVER_ERROR.message,
                ),
            )
    }
}
