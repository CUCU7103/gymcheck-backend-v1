package com.gymcheck.dto.response

import com.gymcheck.domain.user.GoalType

data class StreakResponse(
    val goalType: GoalType,
    val weeklyCount: Int?,
    val currentStreak: Int,
    val longestStreak: Int,
    val currentPeriodAchieved: Boolean,
)
