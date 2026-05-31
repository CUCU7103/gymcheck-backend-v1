package com.gymcheck.service.auth

import com.gymcheck.domain.user.SocialProvider
import com.gymcheck.domain.user.User
import com.gymcheck.dto.response.TokenResponse
import com.gymcheck.exception.CustomException
import com.gymcheck.exception.ErrorCode
import com.gymcheck.repository.UserRepository
import com.gymcheck.security.jwt.JwtProperties
import com.gymcheck.security.jwt.JwtTokenProvider
import com.gymcheck.security.oauth.OAuthClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    /** 등록된 모든 OAuthClient 구현체 (Google, Kakao 등) */
    oAuthClients: List<OAuthClient>,
    private val userRepository: UserRepository,
    private val refreshTokenService: RefreshTokenService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val jwtProperties: JwtProperties,
) {
    /** 프로바이더 enum → 클라이언트 매핑 */
    private val clientMap: Map<SocialProvider, OAuthClient> =
        oAuthClients.associateBy { it.provider }

    @Transactional
    fun processLogin(provider: SocialProvider, code: String): TokenResponse {
        val client = clientMap[provider]
            ?: throw CustomException(ErrorCode.BAD_REQUEST, "지원하지 않는 소셜 프로바이더입니다: $provider")
        val userInfo = client.fetchUserInfo(code)
        val user = findOrCreateUser(provider, userInfo.socialId, userInfo.email, userInfo.nickname)
        return issueTokens(user.id!!)
    }

    @Transactional
    fun refreshAccessToken(refreshToken: String): TokenResponse {
        val userId = refreshTokenService.validateAndGetUserId(refreshToken)
        return TokenResponse(
            accessToken = jwtTokenProvider.createAccessToken(userId),
            refreshToken = refreshToken,
            expiresIn = jwtProperties.accessTokenExpiration / 1000,
        )
    }

    @Transactional
    fun logout(userId: Long) {
        refreshTokenService.revokeAllTokens(userId)
    }

    private fun issueTokens(userId: Long): TokenResponse {
        val accessToken = jwtTokenProvider.createAccessToken(userId)
        val refreshToken = jwtTokenProvider.createRefreshToken(userId)
        refreshTokenService.saveRefreshToken(userId, refreshToken)

        return TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtProperties.accessTokenExpiration / 1000,
        )
    }

    private fun findOrCreateUser(
        provider: SocialProvider,
        socialId: String,
        email: String?,
        nickname: String?,
    ): User {
        return userRepository.findBySocialProviderAndSocialId(provider, socialId)
            ?: userRepository.save(
                User(
                    socialProvider = provider,
                    socialId = socialId,
                    email = email,
                    nickname = nickname ?: email?.substringBefore("@") ?: "${provider.name.lowercase()}-user",
                ),
            )
    }
}
