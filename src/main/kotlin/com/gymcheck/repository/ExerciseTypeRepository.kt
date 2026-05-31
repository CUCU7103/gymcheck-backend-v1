package com.gymcheck.repository

import com.gymcheck.domain.workout.ExerciseType
import org.springframework.data.jpa.repository.JpaRepository

interface ExerciseTypeRepository : JpaRepository<ExerciseType, Long> {
    fun findByIsDefaultTrue(): List<ExerciseType>

    fun findByUserIdOrIsDefaultTrue(userId: Long): List<ExerciseType>

    fun findByIdAndUserId(
        id: Long,
        userId: Long,
    ): ExerciseType?
}
