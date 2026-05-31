package com.gymcheck.dto.response

import com.gymcheck.domain.user.GoalType
import java.time.LocalDateTime

data class GoalResponse(
    val goalType: GoalType,
    val weeklyCount: Int?,
    val updatedAt: LocalDateTime,
)
