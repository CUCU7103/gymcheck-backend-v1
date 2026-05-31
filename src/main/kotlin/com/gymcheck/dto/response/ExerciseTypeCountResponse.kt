package com.gymcheck.dto.response

data class ExerciseTypeCountResponse(
    val exerciseTypeId: Long,
    val exerciseTypeName: String,
    val count: Int,
)
