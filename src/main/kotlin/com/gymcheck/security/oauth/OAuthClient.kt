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
     * 인가 코드를 액세스 토큰으로 교환한 뒤,
     * 사용자 정보까지 조회해 [OAuthUserInfo]로 반환한다.
     */
    fun fetchUserInfo(code: String): OAuthUserInfo
}

/** OAuth 프로바이더에서 받아온 사용자 식별 정보 */
data class OAuthUserInfo(
    /** 프로바이더 고유 사용자 식별자 */
    val socialId: String,
    val email: String?,
    val nickname: String?,
)
