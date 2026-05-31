package com.gymcheck.dto.response

data class MonthlyCalendarResponse(
    val year: Int,
    val month: Int,
    val days: List<CalendarDayResponse>,
)
