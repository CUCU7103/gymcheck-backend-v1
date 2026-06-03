package com.gymcheck.dto.request

import jakarta.validation.constraints.NotBlank

data class GoogleLoginRequest(
    @field:NotBlank
    val idToken: String,
)
