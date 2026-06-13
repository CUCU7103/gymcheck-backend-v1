package com.gymcheck.dto.response

import com.gymcheck.domain.user.GoalType

data class StreakResponse(
    val goalType: GoalType,
    val weeklyCount: Int?,
    val currentStreak: Int,
    val longestStreak: Int,
    val currentPeriodAchieved: Boolean,
    val isGoalAchievedToday: Boolean = currentPeriodAchieved,
    // 목표 달성 여부와 무관하게 오늘 운동 기록이 1개 이상 존재하는지 여부
    val hasLoggedToday: Boolean = false,
)
