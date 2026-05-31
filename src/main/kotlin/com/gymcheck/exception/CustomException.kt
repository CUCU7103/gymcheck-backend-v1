package com.gymcheck.exception

open class CustomException(
    val errorCode: ErrorCode,
    override val message: String? = errorCode.message,
) : RuntimeException(message)
