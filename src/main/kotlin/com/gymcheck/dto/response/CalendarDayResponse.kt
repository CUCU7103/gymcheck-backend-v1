package com.gymcheck.dto.response

import java.time.LocalDate

data class CalendarDayResponse(
    val date: LocalDate,
    val workoutCount: Int,
)
