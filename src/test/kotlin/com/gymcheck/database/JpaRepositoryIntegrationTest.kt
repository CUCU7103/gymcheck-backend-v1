package com.gymcheck.database

import com.gymcheck.domain.notification.FcmToken
import com.gymcheck.domain.notification.NotificationSetting
import com.gymcheck.domain.user.GoalType
import com.gymcheck.domain.user.SocialProvider
import com.gymcheck.domain.user.User
import com.gymcheck.domain.user.UserGoal
import com.gymcheck.domain.workout.ExerciseType
import com.gymcheck.domain.auth.RefreshToken
import com.gymcheck.domain.workout.WorkoutLog
import com.gymcheck.repository.ExerciseTypeRepository
import com.gymcheck.repository.FcmTokenRepository
import com.gymcheck.repository.NotificationSettingRepository
import com.gymcheck.repository.RefreshTokenRepository
import com.gymcheck.repository.UserGoalRepository
import com.gymcheck.repository.UserRepository
import com.gymcheck.repository.WorkoutLogRepository
import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:gymcheck_jpa;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.id.insert_returning_enabled=false",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
    ],
)
@Transactional
class JpaRepositoryIntegrationTest {

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var userGoalRepository: UserGoalRepository

    @Autowired
    lateinit var exerciseTypeRepository: ExerciseTypeRepository

    @Autowired
    lateinit var workoutLogRepository: WorkoutLogRepository

    @Autowired
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    lateinit var fcmTokenRepository: FcmTokenRepository

    @Autowired
    lateinit var notificationSettingRepository: NotificationSettingRepository

    @Test
    fun `default exercise types are seeded`() {
        val defaults = exerciseTypeRepository.findByIsDefaultTrue()

        assertThat(defaults).hasSize(9)
        assertThat(defaults).extracting<String> { it.name }.contains(
            "헬스",
            "러닝",
            "수영",
            "자전거",
            "요가",
            "필라테스",
            "등산",
            "테니스",
            "농구",
        )
    }

    @Test
    fun `repositories persist and load core relationships`() {
        val user = userRepository.save(
            User(
                socialProvider = SocialProvider.GOOGLE,
                socialId = "google-123",
                email = "tester@example.com",
                nickname = "tester",
            ),
        )

        val goal = userGoalRepository.save(
            UserGoal(
                user = user,
                goalType = GoalType.WEEKLY,
                weeklyCount = 3,
            ),
        )

        val customExercise = exerciseTypeRepository.save(
            ExerciseType(
                name = "복싱",
                isDefault = false,
                user = user,
            ),
        )

        val workoutLog = workoutLogRepository.save(
            WorkoutLog(
                user = user,
                exerciseType = customExercise,
                logDate = LocalDate.of(2026, 5, 31),
                memo = "good session",
            ),
        )

        val refreshToken = refreshTokenRepository.save(
            RefreshToken(
                user = user,
                token = "refresh-token",
                expiresAt = LocalDateTime.of(2026, 6, 30, 12, 0),
            ),
        )

        val fcmToken = fcmTokenRepository.save(
            FcmToken(
                user = user,
                token = "fcm-token",
            ),
        )

        val notificationSetting = notificationSettingRepository.save(
            NotificationSetting(
                user = user,
                enabled = true,
                notifyTime = java.time.LocalTime.of(20, 0),
                timezone = "Asia/Seoul",
            ),
        )

        assertThat(user.id).isNotNull
        assertThat(goal.id).isNotNull
        assertThat(customExercise.id).isNotNull
        assertThat(workoutLog.id).isNotNull
        assertThat(refreshToken.id).isNotNull
        assertThat(fcmToken.id).isNotNull
        assertThat(notificationSetting.id).isNotNull

        assertThat(userGoalRepository.findByUserId(user.id!!)).isNotNull
        assertThat(workoutLogRepository.findByUserIdAndLogDate(user.id!!, LocalDate.of(2026, 5, 31)))
            .hasSize(1)
        assertThat(refreshTokenRepository.findByToken("refresh-token")).isNotNull
        assertThat(fcmTokenRepository.findByUserId(user.id!!)).hasSize(1)
        assertThat(notificationSettingRepository.findByUserId(user.id!!)).isNotNull
    }
}
