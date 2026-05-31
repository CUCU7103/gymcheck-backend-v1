package com.gymcheck.service

import com.gymcheck.domain.user.GoalType
import com.gymcheck.domain.user.SocialProvider
import com.gymcheck.domain.user.User
import com.gymcheck.domain.user.UserGoal
import com.gymcheck.domain.workout.WorkoutLog
import com.gymcheck.repository.UserGoalRepository
import com.gymcheck.repository.WorkoutLogRepository
import java.lang.reflect.Field
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class StreakServiceTest {

    private val workoutLogRepository = Mockito.mock(WorkoutLogRepository::class.java)
    private val userGoalRepository = Mockito.mock(UserGoalRepository::class.java)
    private val userFinder = Mockito.mock(UserFinder::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-05-31T12:00:00Z"), ZoneId.of("Asia/Seoul"))

    private val service = StreakService(
        workoutLogRepository = workoutLogRepository,
        userGoalRepository = userGoalRepository,
        userFinder = userFinder,
        clock = clock,
    )

    @Test
    fun `daily streak counts consecutive workout days including today`() {
        val user = user(1L)
        Mockito.`when`(userFinder.findById(1L)).thenReturn(user)
        Mockito.`when`(userGoalRepository.findByUserId(1L)).thenReturn(
            UserGoal(user = user, goalType = GoalType.DAILY),
        )
        Mockito.`when`(workoutLogRepository.findByUserId(1L)).thenReturn(
            listOf(
                workoutLog(user, LocalDate.of(2026, 5, 31)),
                workoutLog(user, LocalDate.of(2026, 5, 30)),
                workoutLog(user, LocalDate.of(2026, 5, 29)),
                workoutLog(user, LocalDate.of(2026, 5, 27)),
            ),
        )

        val response = service.getStreak(1L)

        assertThat(response.currentStreak).isEqualTo(3)
        assertThat(response.longestStreak).isEqualTo(3)
        assertThat(response.currentPeriodAchieved).isTrue()
    }

    @Test
    fun `weekly streak counts consecutive achieved weeks`() {
        val user = user(2L)
        Mockito.`when`(userFinder.findById(2L)).thenReturn(user)
        Mockito.`when`(userGoalRepository.findByUserId(2L)).thenReturn(
            UserGoal(user = user, goalType = GoalType.WEEKLY, weeklyCount = 2),
        )
        Mockito.`when`(workoutLogRepository.findByUserId(2L)).thenReturn(
            listOf(
                workoutLog(user, LocalDate.of(2026, 5, 25)),
                workoutLog(user, LocalDate.of(2026, 5, 26)),
                workoutLog(user, LocalDate.of(2026, 5, 18)),
                workoutLog(user, LocalDate.of(2026, 5, 19)),
            ),
        )

        val response = service.getStreak(2L)

        assertThat(response.currentStreak).isEqualTo(2)
        assertThat(response.longestStreak).isEqualTo(2)
        assertThat(response.currentPeriodAchieved).isTrue()
    }

    private fun user(id: Long): User {
        val user = User(
            socialProvider = SocialProvider.GOOGLE,
            socialId = "social-$id",
            email = "user$id@example.com",
            nickname = "user$id",
        )
        setField(user, "id", id)
        setField(user, "createdAt", LocalDateTime.of(2026, 5, 1, 12, 0))
        return user
    }

    private fun workoutLog(user: User, date: LocalDate): WorkoutLog {
        val exercise = com.gymcheck.domain.workout.ExerciseType(name = "헬스", isDefault = true)
        setField(exercise, "id", 100L)
        setField(exercise, "createdAt", LocalDateTime.of(2026, 5, 1, 12, 0))

        val log = WorkoutLog(
            user = user,
            exerciseType = exercise,
            logDate = date,
            memo = null,
        )
        setField(log, "id", date.toEpochDay())
        setField(log, "createdAt", LocalDateTime.of(2026, 5, 1, 12, 0))
        return log
    }

    private fun setField(target: Any, fieldName: String, value: Any?) {
        var type: Class<*>? = target.javaClass
        var field: Field? = null
        while (type != null && field == null) {
            field = try {
                type.getDeclaredField(fieldName)
            } catch (_: NoSuchFieldException) {
                null
            }
            type = type.superclass
        }
        requireNotNull(field) { "Field $fieldName not found" }
        field.isAccessible = true
        field.set(target, value)
    }
}
