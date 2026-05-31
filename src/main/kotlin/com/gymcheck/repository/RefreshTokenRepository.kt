package com.gymcheck.repository

import com.gymcheck.domain.workout.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): RefreshToken?

    fun findByUserId(userId: Long): List<RefreshToken>

    fun deleteByUserId(userId: Long)
}
