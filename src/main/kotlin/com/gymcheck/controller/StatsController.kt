package com.gymcheck.controller

import com.gymcheck.dto.response.MonthlyCalendarResponse
import com.gymcheck.dto.response.StreakResponse
import com.gymcheck.dto.response.StatisticsSummaryResponse
import com.gymcheck.service.StreakService
import com.gymcheck.service.StatisticsService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import com.gymcheck.security.jwt.UserPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal

@RestController
@RequestMapping("/stats")
class StatsController(
    private val streakService: StreakService,
    private val statisticsService: StatisticsService,
) {

    @GetMapping("/streak")
    fun getStreak(
        @AuthenticationPrincipal user: UserPrincipal,
    ): StreakResponse = streakService.getStreak(user.id)

    @GetMapping("/calendar")
    fun getCalendar(
        @AuthenticationPrincipal user: UserPrincipal,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): MonthlyCalendarResponse = statisticsService.getMonthlyCalendar(user.id, year, month)

    @GetMapping("/summary")
    fun getSummary(
        @AuthenticationPrincipal user: UserPrincipal,
        @RequestParam year: Int,
        @RequestParam month: Int,
    ): StatisticsSummaryResponse = statisticsService.getSummary(user.id, year, month)
}
