package com.gymcheck.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterFcmTokenRequest(
    @field:NotBlank
    @field:Size(max = 512)
    val token: String,
)
