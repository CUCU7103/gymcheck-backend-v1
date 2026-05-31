package com.gymcheck.repository

import com.gymcheck.domain.workout.WorkoutLog
import java.time.LocalDate
import org.springframework.data.jpa.repository.JpaRepository

interface WorkoutLogRepository : JpaRepository<WorkoutLog, Long> {
    fun findByUserId(userId: Long): List<WorkoutLog>

    fun findByUserIdAndLogDateBetween(
        userId: Long,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<WorkoutLog>

    fun findByUserIdAndLogDate(
        userId: Long,
        logDate: LocalDate,
    ): List<WorkoutLog>

    fun findByIdAndUserId(
        id: Long,
        userId: Long,
    ): WorkoutLog?
}
