package com.gymcheck.service.auth

import com.gymcheck.domain.auth.RefreshToken
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
    /**
     * 사용자당 하나의 refresh token만 보관한다.
     * 모바일 앱에서 중복 로그인 정책이 바뀌면 이 메서드와 DB unique 정책을 함께 검토해야 한다.
     */
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

    /**
     * refresh token 검증은 두 단계다.
     * 1) JWT 자체의 서명/만료 검증, 2) 서버 저장소에 아직 남아 있는 토큰인지 확인.
     */
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
