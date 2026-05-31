package com.gymcheck.dto.request

import jakarta.validation.constraints.Size

data class UpdateProfileRequest(
    @field:Size(min = 2, max = 20, message = "닉네임은 2~20자 사이여야 합니다")
    val nickname: String? = null,
)
