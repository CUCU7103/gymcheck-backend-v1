package com.gymcheck.exception

import java.time.LocalDateTime

data class ApiErrorResponse(
    val code: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
)
