package com.gymcheck.service

import com.gymcheck.domain.user.GoalType
import com.gymcheck.dto.response.CalendarDayResponse
import com.gymcheck.dto.response.ExerciseTypeCountResponse
import com.gymcheck.dto.response.ExerciseTypeResponse
import com.gymcheck.dto.response.ExerciseTypeStatResponse
import com.gymcheck.dto.response.MonthlyCalendarResponse
import com.gymcheck.dto.response.StatisticsSummaryResponse
import com.gymcheck.dto.response.WeeklyProgressResponse
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

    /**
     * 월간 캘린더는 날짜별 운동 기록 개수만 제공한다.
     * 프론트엔드는 이 응답으로 달력 셀의 체크 여부나 배지를 그린다.
     */
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

    /**
     * 월간 요약은 운동 종류별 집계와 "이번 주 목표 달성률"을 함께 반환한다.
     * weeklyProgress는 선택한 월에 따라 계산 기준 주가 달라지므로 아래 calculateWeeklyProgress를 함께 확인해야 한다.
     */
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

        val exerciseTypeStats = exerciseTypeCounts.map { count ->
            val exerciseType = logs.first { it.exerciseType.id == count.exerciseTypeId }.exerciseType
            ExerciseTypeStatResponse(
                exerciseType = ExerciseTypeResponse(
                    id = count.exerciseTypeId,
                    name = count.exerciseTypeName,
                    isDefault = exerciseType.isDefault,
                    userId = exerciseType.user?.id,
                    usageCount = 0,
                ),
                count = count.count,
            )
        }

        val weeklyProgress = calculateWeeklyProgress(userId, yearMonth)

        return StatisticsSummaryResponse(
            totalWorkoutCount = logs.size,
            exerciseTypeCounts = exerciseTypeCounts,
            weeklyAchievementRate = weeklyProgress.percentage,
            weeklyProgress = weeklyProgress,
            exerciseTypeStats = exerciseTypeStats,
            monthlyTotal = logs.size,
        )
    }

    private fun calculateWeeklyProgress(userId: Long, yearMonth: YearMonth): WeeklyProgressResponse {
        val goal = userGoalRepository.findByUserId(userId)
        val today = LocalDate.now(clock)
        val currentYearMonth = YearMonth.from(today)

        val (weekStart, weekEnd) = when {
            yearMonth.isAfter(currentYearMonth) -> return WeeklyProgressResponse(current = 0, goal = goalTarget(goal), percentage = 0.0)
            yearMonth == currentYearMonth -> {
                // 현재 월: 오늘이 속한 월~일 주간을 기준으로 진행률을 계산한다.
                today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to
                    today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            }
            else -> {
                // 과거 월: 해당 월의 마지막 날을 포함하는 주(월~마지막 날) 기준.
                // 월 경계 밖의 다음 달 기록이 과거 월 요약에 섞이지 않도록 weekEnd를 lastDay로 제한한다.
                val lastDay = yearMonth.atEndOfMonth()
                lastDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to lastDay
            }
        }

        val logs = workoutLogRepository.findByUserIdAndLogDateBetween(userId, weekStart, weekEnd)
        val count = logs.size
        val distinctDays = logs.map { it.logDate }.toSet().size

        val current = when (goal?.goalType ?: GoalType.DAILY) {
            GoalType.DAILY -> distinctDays
            GoalType.WEEKLY -> count
        }
        val target = goalTarget(goal)

        return WeeklyProgressResponse(
            current = current,
            goal = target,
            percentage = (current.toDouble() / target).coerceAtMost(1.0),
        )
    }

    private fun goalTarget(goal: com.gymcheck.domain.user.UserGoal?): Int =
        when (goal?.goalType ?: GoalType.DAILY) {
            GoalType.DAILY -> 7
            GoalType.WEEKLY -> goal?.weeklyCount ?: 1
        }
}
