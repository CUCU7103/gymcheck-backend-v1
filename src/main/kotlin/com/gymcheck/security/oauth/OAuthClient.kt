package com.gymcheck.security.oauth

import com.gymcheck.domain.user.SocialProvider

/**
 * 소셜 OAuth 프로바이더의 공통 계약.
 * Google, Kakao 등 모든 프로바이더는 이 인터페이스를 구현한다.
 */
interface OAuthClient {
    /** 이 클라이언트가 담당하는 소셜 프로바이더 */
    val provider: SocialProvider

    /**
     * 프로바이더 자격증명(credential)으로 사용자 정보를 조회해 [OAuthUserInfo]로 반환한다.
     *
     * credential의 구체적 형태는 프로바이더마다 다르다.
     * - Google: 클라이언트가 발급받은 ID 토큰(JWT)
     * - Kakao: OAuth 인가 코드(authorization code)
     *
     * 각 구현체가 자신의 방식으로 검증·교환해 사용자 정보를 얻는다.
     */
    fun fetchUserInfo(credential: String): OAuthUserInfo
}

/** OAuth 프로바이더에서 받아온 사용자 식별 정보 */
data class OAuthUserInfo(
    /** 프로바이더 고유 사용자 식별자 */
    val socialId: String,
    val email: String?,
    val nickname: String?,
)
