package com.gymcheck.dto.request

import com.gymcheck.domain.user.GoalType
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

data class UpdateGoalRequest(
    @field:NotNull(message = "목표 타입은 필수입니다")
    val goalType: GoalType,

    @field:Min(1, message = "주간 목표 횟수는 1 이상이어야 합니다")
    @field:Max(7, message = "주간 목표 횟수는 7 이하여야 합니다")
    val weeklyCount: Int? = null,
)
