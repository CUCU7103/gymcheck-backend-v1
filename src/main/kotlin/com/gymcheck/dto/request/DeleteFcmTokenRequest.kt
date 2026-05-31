package com.gymcheck.dto.request

import jakarta.validation.constraints.NotBlank

data class DeleteFcmTokenRequest(
    @field:NotBlank
    val token: String,
)
