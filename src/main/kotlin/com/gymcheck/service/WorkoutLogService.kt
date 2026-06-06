package com.gymcheck.service

import com.gymcheck.domain.workout.ExerciseType
import com.gymcheck.domain.workout.WorkoutLog
import com.gymcheck.dto.request.CreateWorkoutLogRequest
import com.gymcheck.dto.response.ExerciseTypeResponse
import com.gymcheck.dto.response.WorkoutLogResponse
import com.gymcheck.exception.CustomException
import com.gymcheck.exception.ErrorCode
import com.gymcheck.repository.ExerciseTypeRepository
import com.gymcheck.repository.WorkoutLogRepository
import java.time.LocalDate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WorkoutLogService(
    private val workoutLogRepository: WorkoutLogRepository,
    private val exerciseTypeRepository: ExerciseTypeRepository,
    private val userFinder: UserFinder,
) {

    /**
     * 하루에 여러 운동을 기록할 수 있으므로 목록 반환값은 복수다.
     * 최신 입력 순으로 보여주기 위해 createdAt/id 역순 정렬을 적용한다.
     */
    @Transactional(readOnly = true)
    fun getWorkoutLogs(userId: Long, logDate: LocalDate): List<WorkoutLogResponse> {
        return workoutLogRepository.findByUserIdAndLogDate(userId, logDate)
            .sortedWith(compareByDescending<WorkoutLog> { it.createdAt }.thenByDescending { it.id })
            .map { it.toResponse() }
    }

    /**
     * 로그 생성 시 운동 종류 접근 권한을 먼저 확인한다.
     * 기본 운동은 모두 접근 가능하지만, 커스텀 운동은 소유자만 사용할 수 있다.
     */
    @Transactional
    fun createWorkoutLog(userId: Long, request: CreateWorkoutLogRequest): WorkoutLogResponse {
        val user = userFinder.findById(userId)
        val exerciseType = findAccessibleExerciseType(userId, request.exerciseTypeId)

        val saved = workoutLogRepository.save(
            WorkoutLog(
                user = user,
                exerciseType = exerciseType,
                logDate = request.logDate,
                memo = request.memo,
            ),
        )

        return saved.toResponse()
    }

    @Transactional
    fun deleteWorkoutLog(userId: Long, workoutLogId: Long) {
        val log = workoutLogRepository.findByIdAndUserId(workoutLogId, userId)
            ?: throw CustomException(ErrorCode.NOT_FOUND, "체크인 기록을 찾을 수 없습니다.")
        workoutLogRepository.delete(log)
    }

    private fun findAccessibleExerciseType(userId: Long, exerciseTypeId: Long): ExerciseType {
        val exerciseType = exerciseTypeRepository.findById(exerciseTypeId)
            .orElseThrow { CustomException(ErrorCode.NOT_FOUND, "운동 종류를 찾을 수 없습니다.") }

        if (!exerciseType.isDefault && exerciseType.user?.id != userId) {
            throw CustomException(ErrorCode.FORBIDDEN, "운동 종류에 접근할 수 없습니다.")
        }

        return exerciseType
    }

    private fun WorkoutLog.toResponse() = WorkoutLogResponse(
        id = id!!,
        exerciseType = ExerciseTypeResponse(
            id = exerciseType.id!!,
            name = exerciseType.name,
            isDefault = exerciseType.isDefault,
            userId = exerciseType.user?.id,
            usageCount = 0,
        ),
        logDate = logDate,
        memo = memo,
        createdAt = createdAt!!,
    )
}
