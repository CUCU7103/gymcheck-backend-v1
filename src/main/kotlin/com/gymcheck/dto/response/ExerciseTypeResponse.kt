package com.gymcheck.dto.response

data class ExerciseTypeResponse(
    val id: Long,
    val name: String,
    val isDefault: Boolean,
    val userId: Long?,
    val usageCount: Long,
)
