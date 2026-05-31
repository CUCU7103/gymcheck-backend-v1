package com.gymcheck.repository

import com.gymcheck.domain.notification.FcmToken
import org.springframework.data.jpa.repository.JpaRepository

interface FcmTokenRepository : JpaRepository<FcmToken, Long> {
    fun findByUserId(userId: Long): List<FcmToken>

    fun findByUserIdAndToken(
        userId: Long,
        token: String,
    ): FcmToken?

    fun deleteByUserIdAndToken(userId: Long, token: String)
}
