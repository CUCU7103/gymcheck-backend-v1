package com.gymcheck.dto.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PastOrPresent
import java.time.LocalDate

data class CreateWorkoutLogRequest(
    @field:NotNull
    @field:Positive
    val exerciseTypeId: Long,
    @field:NotNull
    @field:PastOrPresent
    val logDate: LocalDate,
    val memo: String? = null,
)
