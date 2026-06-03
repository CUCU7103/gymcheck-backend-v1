package com.gymcheck.dto.response

import java.time.LocalDate
import java.time.LocalDateTime

data class WorkoutLogResponse(
    val id: Long,
    val exerciseType: ExerciseTypeResponse,
    val logDate: LocalDate,
    val memo: String?,
    val createdAt: LocalDateTime,
)
