package com.gymcheck.service

import com.gymcheck.domain.user.GoalType
import com.gymcheck.domain.user.UserGoal
import com.gymcheck.dto.request.UpdateGoalRequest
import com.gymcheck.dto.request.UpdateProfileRequest
import com.gymcheck.dto.response.GoalResponse
import com.gymcheck.dto.response.UserProfileResponse
import com.gymcheck.exception.CustomException
import com.gymcheck.exception.ErrorCode
import com.gymcheck.repository.UserGoalRepository
import com.gymcheck.repository.UserRepository
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val userGoalRepository: UserGoalRepository,
    private val userFinder: UserFinder,
) {

    @Transactional(readOnly = true)
    fun getProfile(userId: Long): UserProfileResponse = userFinder.findById(userId).toProfileResponse()

    @Transactional
    fun updateProfile(userId: Long, request: UpdateProfileRequest): UserProfileResponse {
        val user = userFinder.findById(userId)
        request.nickname?.let { user.nickname = it }
        return user.toProfileResponse()
    }

    @Transactional(readOnly = true)
    fun getGoal(userId: Long): GoalResponse {
        val goal = userGoalRepository.findByUserId(userId) ?: return GoalResponse(
            goalType = GoalType.DAILY,
            weeklyCount = null,
            updatedAt = LocalDateTime.now(),
        )
        return goal.toResponse()
    }

    @Transactional
    fun updateGoal(userId: Long, request: UpdateGoalRequest): GoalResponse {
        if (request.goalType == GoalType.WEEKLY && request.weeklyCount == null) {
            throw CustomException(ErrorCode.BAD_REQUEST, "주간 목표는 횟수를 지정해야 합니다")
        }

        val user = userFinder.findById(userId)
        val goal = userGoalRepository.findByUserId(userId)
            ?: UserGoal(user = user, goalType = request.goalType, weeklyCount = request.weeklyCount)

        goal.goalType = request.goalType
        goal.weeklyCount = if (request.goalType == GoalType.WEEKLY) request.weeklyCount else null

        return userGoalRepository.save(goal).toResponse()
    }

    @Transactional
    fun deleteAccount(userId: Long) {
        // cascade = ALL + orphanRemoval = true 설정으로 User 삭제 시 자식 엔티티 자동 삭제 (Task 3)
        val user = userFinder.findById(userId)
        userRepository.delete(user)
    }

    private fun com.gymcheck.domain.user.User.toProfileResponse() = UserProfileResponse(
        id = id!!,
        email = email,
        nickname = nickname,
        socialProvider = socialProvider,
        createdAt = createdAt!!,
    )

    private fun UserGoal.toResponse() = GoalResponse(
        goalType = goalType,
        weeklyCount = weeklyCount,
        updatedAt = createdAt!!,
    )
}
