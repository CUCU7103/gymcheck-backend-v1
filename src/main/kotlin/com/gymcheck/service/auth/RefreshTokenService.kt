package com.gymcheck.service.auth

import com.gymcheck.domain.workout.RefreshToken
import com.gymcheck.exception.CustomException
import com.gymcheck.exception.ErrorCode
import com.gymcheck.repository.RefreshTokenRepository
import com.gymcheck.repository.UserRepository
import com.gymcheck.security.jwt.JwtTokenProvider
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userRepository: UserRepository,
    private val jwtTokenProvider: JwtTokenProvider,
) {
    @Transactional
    fun saveRefreshToken(userId: Long, token: String): RefreshToken {
        refreshTokenRepository.deleteByUserId(userId)

        val user = userRepository.findById(userId)
            .orElseThrow { CustomException(ErrorCode.UNAUTHORIZED, "사용자를 찾을 수 없습니다.") }

        val refreshToken = RefreshToken(
            user = user,
            token = token,
            expiresAt = jwtTokenProvider.getExpiration(token),
        )

        return refreshTokenRepository.save(refreshToken)
    }

    @Transactional(readOnly = true)
    fun validateAndGetUserId(token: String): Long {
        if (!jwtTokenProvider.validateToken(token)) {
            throw CustomException(ErrorCode.UNAUTHORIZED, "Refresh token이 유효하지 않습니다.")
        }

        val stored = refreshTokenRepository.findByToken(token)
            ?: throw CustomException(ErrorCode.UNAUTHORIZED, "Refresh token을 찾을 수 없습니다.")

        if (stored.expiresAt.isBefore(LocalDateTime.now())) {
            throw CustomException(ErrorCode.UNAUTHORIZED, "Refresh token이 만료되었습니다.")
        }

        return jwtTokenProvider.getUserIdFromToken(token)
    }

    @Transactional
    fun revokeAllTokens(userId: Long) {
        refreshTokenRepository.deleteByUserId(userId)
    }
}
