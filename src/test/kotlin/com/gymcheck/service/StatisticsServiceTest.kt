package com.gymcheck.service

import com.gymcheck.domain.user.GoalType
import com.gymcheck.domain.user.SocialProvider
import com.gymcheck.domain.user.User
import com.gymcheck.domain.user.UserGoal
import com.gymcheck.domain.workout.ExerciseType
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

class StatisticsServiceTest {

    private val workoutLogRepository = Mockito.mock(WorkoutLogRepository::class.java)
    private val userGoalRepository = Mockito.mock(UserGoalRepository::class.java)
    private val userFinder = Mockito.mock(UserFinder::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-05-31T12:00:00Z"), ZoneId.of("Asia/Seoul"))

    private val service = StatisticsService(
        workoutLogRepository = workoutLogRepository,
        userGoalRepository = userGoalRepository,
        userFinder = userFinder,
        clock = clock,
    )

    @Test
    fun `monthly calendar marks days with workout counts`() {
        val user = user(1L)
        Mockito.`when`(userFinder.findById(1L)).thenReturn(user)
        Mockito.`when`(workoutLogRepository.findByUserIdAndLogDateBetween(1L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
            .thenReturn(
                listOf(
                    workoutLog(user, "헬스", true, LocalDate.of(2026, 5, 30)),
                    workoutLog(user, "헬스", true, LocalDate.of(2026, 5, 31)),
                    workoutLog(user, "러닝", true, LocalDate.of(2026, 5, 31)),
                ),
            )

        val response = service.getMonthlyCalendar(1L, 2026, 5)

        assertThat(response.days.first { it.date == LocalDate.of(2026, 5, 30) }.workoutCount).isEqualTo(1)
        assertThat(response.days.first { it.date == LocalDate.of(2026, 5, 31) }.workoutCount).isEqualTo(2)
    }

    @Test
    fun `summary returns exercise counts and weekly achievement rate`() {
        val user = user(2L)
        Mockito.`when`(userFinder.findById(2L)).thenReturn(user)
        Mockito.`when`(userGoalRepository.findByUserId(2L)).thenReturn(
            UserGoal(user = user, goalType = GoalType.WEEKLY, weeklyCount = 3),
        )
        Mockito.`when`(workoutLogRepository.findByUserIdAndLogDateBetween(2L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)))
            .thenReturn(
                listOf(
                    workoutLog(user, "헬스", true, LocalDate.of(2026, 5, 30)),
                    workoutLog(user, "헬스", true, LocalDate.of(2026, 5, 31)),
                    workoutLog(user, "러닝", true, LocalDate.of(2026, 5, 31)),
                ),
            )
        Mockito.`when`(workoutLogRepository.findByUserIdAndLogDateBetween(2L, LocalDate.of(2026, 5, 25), LocalDate.of(2026, 5, 31)))
            .thenReturn(
                listOf(
                    workoutLog(user, "헬스", true, LocalDate.of(2026, 5, 30)),
                    workoutLog(user, "헬스", true, LocalDate.of(2026, 5, 31)),
                    workoutLog(user, "러닝", true, LocalDate.of(2026, 5, 31)),
                ),
            )

        val response = service.getSummary(2L, 2026, 5)

        assertThat(response.totalWorkoutCount).isEqualTo(3)
        assertThat(response.exerciseTypeCounts).hasSize(2)
        assertThat(response.exerciseTypeCounts.first().exerciseTypeName).isEqualTo("헬스")
        assertThat(response.weeklyAchievementRate).isEqualTo(1.0)
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

    private fun workoutLog(user: User, name: String, isDefault: Boolean, date: LocalDate): WorkoutLog {
        val exercise = ExerciseType(name = name, isDefault = isDefault)
        setField(exercise, "id", if (name == "헬스") 100L else 101L)
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
