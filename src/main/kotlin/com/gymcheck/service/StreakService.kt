package com.gymcheck.service

import com.gymcheck.domain.user.GoalType
import com.gymcheck.dto.response.StreakResponse
import com.gymcheck.repository.UserGoalRepository
import com.gymcheck.repository.WorkoutLogRepository
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StreakService(
    private val workoutLogRepository: WorkoutLogRepository,
    private val userGoalRepository: UserGoalRepository,
    private val userFinder: UserFinder,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun getStreak(userId: Long): StreakResponse {
        userFinder.findById(userId)
        val goal = userGoalRepository.findByUserId(userId)

        val response = when (goal?.goalType ?: GoalType.DAILY) {
            GoalType.DAILY -> calculateDailyStreak(userId)
            GoalType.WEEKLY -> calculateWeeklyStreak(userId, goal!!.weeklyCount ?: 1)
        }

        return StreakResponse(
            goalType = goal?.goalType ?: GoalType.DAILY,
            weeklyCount = goal?.weeklyCount,
            currentStreak = response.currentStreak,
            longestStreak = response.longestStreak,
            currentPeriodAchieved = response.currentPeriodAchieved,
        )
    }

    private fun calculateDailyStreak(userId: Long): StreakCalculation {
        val logs = workoutLogRepository.findByUserId(userId)
            .map { it.logDate }
            .toSet()
        val today = LocalDate.now(clock)

        val currentStreak = generateSequence(today) { it.minusDays(1) }
            .takeWhile { logs.contains(it) }
            .count()

        val longestStreak = longestConsecutiveDays(logs)

        return StreakCalculation(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            currentPeriodAchieved = logs.contains(today),
        )
    }

    private fun calculateWeeklyStreak(userId: Long, weeklyCount: Int): StreakCalculation {
        val logs = workoutLogRepository.findByUserId(userId)
        val weekCounts = logs.groupingBy { startOfWeek(it.logDate) }.eachCount()
        val currentWeekStart = startOfWeek(LocalDate.now(clock))
        val currentWeekAchieved = (weekCounts[currentWeekStart] ?: 0) >= weeklyCount

        val currentStreak = if (!currentWeekAchieved) {
            0
        } else {
            generateSequence(currentWeekStart) { it.minusWeeks(1) }
                .takeWhile { (weekCounts[it] ?: 0) >= weeklyCount }
                .count()
        }

        val longestStreak = longestConsecutiveWeeks(weekCounts, weeklyCount)

        return StreakCalculation(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            currentPeriodAchieved = currentWeekAchieved,
        )
    }

    private fun longestConsecutiveDays(logDates: Set<LocalDate>): Int {
        if (logDates.isEmpty()) return 0
        val sorted = logDates.sorted()
        var longest = 1
        var current = 1
        for (index in 1 until sorted.size) {
            if (sorted[index] == sorted[index - 1].plusDays(1)) current += 1 else current = 1
            longest = maxOf(longest, current)
        }
        return longest
    }

    private fun longestConsecutiveWeeks(weekCounts: Map<LocalDate, Int>, weeklyCount: Int): Int {
        if (weekCounts.isEmpty()) return 0
        val achievedWeeks = weekCounts.filterValues { it >= weeklyCount }.keys.sorted()
        if (achievedWeeks.isEmpty()) return 0
        var longest = 1
        var current = 1
        for (index in 1 until achievedWeeks.size) {
            if (achievedWeeks[index] == achievedWeeks[index - 1].plusWeeks(1)) current += 1 else current = 1
            longest = maxOf(longest, current)
        }
        return longest
    }

    private fun startOfWeek(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private data class StreakCalculation(
        val currentStreak: Int,
        val longestStreak: Int,
        val currentPeriodAchieved: Boolean,
    )
}
