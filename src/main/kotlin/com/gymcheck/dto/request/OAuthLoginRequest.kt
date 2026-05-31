package com.gymcheck.dto.request

import jakarta.validation.constraints.NotBlank

data class OAuthLoginRequest(
    @field:NotBlank
    val code: String,
    val redirectUri: String? = null,
)
