package com.gymcheck.service

import com.gymcheck.dto.response.CalendarDayResponse
import com.gymcheck.dto.response.ExerciseTypeCountResponse
import com.gymcheck.dto.response.MonthlyCalendarResponse
import com.gymcheck.dto.response.StatisticsSummaryResponse
import com.gymcheck.repository.UserGoalRepository
import com.gymcheck.repository.WorkoutLogRepository
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StatisticsService(
    private val workoutLogRepository: WorkoutLogRepository,
    private val userGoalRepository: UserGoalRepository,
    private val userFinder: UserFinder,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun getMonthlyCalendar(userId: Long, year: Int, month: Int): MonthlyCalendarResponse {
        userFinder.findById(userId)
        val yearMonth = YearMonth.of(year, month)
        val logs = workoutLogRepository.findByUserIdAndLogDateBetween(
            userId,
            yearMonth.atDay(1),
            yearMonth.atEndOfMonth(),
        )

        val countByDate = logs.groupingBy { it.logDate }.eachCount()
        val days = (1..yearMonth.lengthOfMonth()).map { day ->
            val date = yearMonth.atDay(day)
            CalendarDayResponse(
                date = date,
                workoutCount = countByDate[date] ?: 0,
            )
        }

        return MonthlyCalendarResponse(year = year, month = month, days = days)
    }

    @Transactional(readOnly = true)
    fun getSummary(userId: Long, year: Int, month: Int): StatisticsSummaryResponse {
        userFinder.findById(userId)
        val yearMonth = YearMonth.of(year, month)
        val logs = workoutLogRepository.findByUserIdAndLogDateBetween(
            userId,
            yearMonth.atDay(1),
            yearMonth.atEndOfMonth(),
        )

        val exerciseTypeCounts = logs
            .groupingBy { it.exerciseType.id!! }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { (exerciseTypeId, count) ->
                val exerciseType = logs.first { it.exerciseType.id == exerciseTypeId }.exerciseType
                ExerciseTypeCountResponse(
                    exerciseTypeId = exerciseTypeId,
                    exerciseTypeName = exerciseType.name,
                    count = count,
                )
            }

        val weeklyAchievementRate = calculateWeeklyAchievementRate(userId, yearMonth)

        return StatisticsSummaryResponse(
            totalWorkoutCount = logs.size,
            exerciseTypeCounts = exerciseTypeCounts,
            weeklyAchievementRate = weeklyAchievementRate,
        )
    }

    private fun calculateWeeklyAchievementRate(userId: Long, yearMonth: YearMonth): Double {
        val goal = userGoalRepository.findByUserId(userId) ?: return 0.0
        val today = LocalDate.now(clock)
        val currentYearMonth = YearMonth.from(today)

        val (weekStart, weekEnd) = when {
            yearMonth.isAfter(currentYearMonth) -> return 0.0
            yearMonth == currentYearMonth -> {
                today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to
                    today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            }
            else -> {
                // 과거 월: 해당 월의 마지막 날을 포함하는 주(월~마지막 날) 기준
                val lastDay = yearMonth.atEndOfMonth()
                lastDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to lastDay
            }
        }

        val logs = workoutLogRepository.findByUserIdAndLogDateBetween(userId, weekStart, weekEnd)
        val count = logs.size
        val distinctDays = logs.map { it.logDate }.toSet().size

        return when (goal.goalType) {
            com.gymcheck.domain.user.GoalType.DAILY -> (distinctDays.toDouble() / 7.0).coerceAtMost(1.0)
            com.gymcheck.domain.user.GoalType.WEEKLY -> {
                val target = goal.weeklyCount ?: 1
                (count.toDouble() / target).coerceAtMost(1.0)
            }
        }
    }
}
