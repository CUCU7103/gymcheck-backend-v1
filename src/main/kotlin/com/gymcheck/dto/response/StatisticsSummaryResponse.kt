package com.gymcheck.dto.response

data class StatisticsSummaryResponse(
    val totalWorkoutCount: Int,
    val exerciseTypeCounts: List<ExerciseTypeCountResponse>,
    val weeklyAchievementRate: Double,
)
