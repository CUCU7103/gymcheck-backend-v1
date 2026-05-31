package com.gymcheck.repository

import com.gymcheck.domain.user.UserGoal
import org.springframework.data.jpa.repository.JpaRepository

interface UserGoalRepository : JpaRepository<UserGoal, Long> {
    fun findByUserId(userId: Long): UserGoal?
}
