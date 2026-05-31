package com.gymcheck.service

import com.gymcheck.domain.workout.ExerciseType
import com.gymcheck.dto.request.CreateExerciseTypeRequest
import com.gymcheck.dto.request.UpdateExerciseTypeRequest
import com.gymcheck.dto.response.ExerciseTypeResponse
import com.gymcheck.exception.CustomException
import com.gymcheck.exception.ErrorCode
import com.gymcheck.repository.ExerciseTypeRepository
import com.gymcheck.repository.WorkoutLogRepository
import java.time.LocalDate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ExerciseTypeService(
    private val exerciseTypeRepository: ExerciseTypeRepository,
    private val workoutLogRepository: WorkoutLogRepository,
    private val userFinder: UserFinder,
) {

    @Transactional(readOnly = true)
    fun getExerciseTypes(userId: Long): List<ExerciseTypeResponse> {
        val types = exerciseTypeRepository.findByUserIdOrIsDefaultTrue(userId)
        val usageCounts = countUsage(userId)

        return types
            .sortedWith(
                compareByDescending<ExerciseType> { usageCounts[it.id] ?: 0L }
                    .thenByDescending { it.isDefault }
                    .thenBy { it.name.lowercase() },
            )
            .map { type ->
                type.toResponse(usageCounts[type.id] ?: 0L)
            }
    }

    @Transactional
    fun createCustomExerciseType(userId: Long, request: CreateExerciseTypeRequest): ExerciseTypeResponse {
        val user = userFinder.findById(userId)
        ensureNameAvailable(userId, request.name)

        val saved = exerciseTypeRepository.save(
            ExerciseType(
                name = request.name.trim(),
                isDefault = false,
                user = user,
            ),
        )

        return saved.toResponse(0L)
    }

    @Transactional
    fun updateCustomExerciseType(
        userId: Long,
        exerciseTypeId: Long,
        request: UpdateExerciseTypeRequest,
    ): ExerciseTypeResponse {
        val exerciseType = findCustomExerciseType(userId, exerciseTypeId)
        ensureNameAvailable(userId, request.name, excludeId = exerciseTypeId)
        exerciseType.name = request.name.trim()
        val saved = exerciseTypeRepository.save(exerciseType)
        val usageCount = countUsage(userId)[saved.id] ?: 0L
        return saved.toResponse(usageCount)
    }

    @Transactional
    fun deleteCustomExerciseType(userId: Long, exerciseTypeId: Long) {
        val exerciseType = findCustomExerciseType(userId, exerciseTypeId)
        exerciseTypeRepository.delete(exerciseType)
    }

    private fun countUsage(userId: Long): Map<Long, Long> {
        val startDate = LocalDate.now().minusDays(30)
        val logs = workoutLogRepository.findByUserIdAndLogDateBetween(userId, startDate, LocalDate.now())
        return logs.groupingBy { it.exerciseType.id!! }.eachCount().mapValues { it.value.toLong() }
    }

    private fun findCustomExerciseType(userId: Long, exerciseTypeId: Long): ExerciseType {
        val exerciseType = exerciseTypeRepository.findByIdAndUserId(exerciseTypeId, userId)
            ?: throw CustomException(ErrorCode.NOT_FOUND, "운동 종류를 찾을 수 없습니다.")

        if (exerciseType.isDefault) {
            throw CustomException(ErrorCode.FORBIDDEN, "기본 운동 종류는 수정할 수 없습니다.")
        }

        return exerciseType
    }

    private fun ensureNameAvailable(userId: Long, name: String, excludeId: Long? = null) {
        val normalized = name.trim().lowercase()
        val exists = exerciseTypeRepository.findByUserIdOrIsDefaultTrue(userId)
            .any { type ->
                type.id != excludeId && type.name.trim().lowercase() == normalized
            }

        if (exists) {
            throw CustomException(ErrorCode.CONFLICT, "이미 같은 이름의 운동 종류가 있습니다.")
        }
    }

    private fun ExerciseType.toResponse(usageCount: Long) = ExerciseTypeResponse(
        id = id!!,
        name = name,
        isDefault = isDefault,
        userId = user?.id,
        usageCount = usageCount,
    )
}
