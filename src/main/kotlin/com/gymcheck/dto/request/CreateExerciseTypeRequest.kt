package com.gymcheck.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateExerciseTypeRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
)
