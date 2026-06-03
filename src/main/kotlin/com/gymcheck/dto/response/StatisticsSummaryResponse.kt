package com.gymcheck.dto.response

data class StatisticsSummaryResponse(
    val totalWorkoutCount: Int,
    val exerciseTypeCounts: List<ExerciseTypeCountResponse>,
    val weeklyAchievementRate: Double,
    val weeklyProgress: WeeklyProgressResponse,
    val exerciseTypeStats: List<ExerciseTypeStatResponse> = exerciseTypeCounts.map { it.toExerciseTypeStatResponse() },
    val monthlyTotal: Int = totalWorkoutCount,
)

data class WeeklyProgressResponse(
    val current: Int,
    val goal: Int,
    val percentage: Double,
)

data class ExerciseTypeStatResponse(
    val exerciseType: ExerciseTypeResponse,
    val count: Int,
)

private fun ExerciseTypeCountResponse.toExerciseTypeStatResponse(): ExerciseTypeStatResponse =
    ExerciseTypeStatResponse(
        exerciseType = ExerciseTypeResponse(
            id = exerciseTypeId,
            name = exerciseTypeName,
            isDefault = true,
            userId = null,
            usageCount = 0,
        ),
        count = count,
    )
