package com.gymcheck.dto.response

import com.gymcheck.domain.user.SocialProvider
import java.time.LocalDateTime

data class UserProfileResponse(
    val id: Long,
    val email: String?,
    val nickname: String?,
    val socialProvider: SocialProvider,
    val createdAt: LocalDateTime,
)
