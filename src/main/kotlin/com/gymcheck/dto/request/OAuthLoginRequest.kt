package com.gymcheck.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class OAuthLoginRequest(
    @field:NotBlank
    @Schema(description = "카카오 OAuth 인가 코드")
    val code: String,
    @Schema(description = "OAuth 리디렉션 URI (선택)")
    val redirectUri: String? = null,
)
