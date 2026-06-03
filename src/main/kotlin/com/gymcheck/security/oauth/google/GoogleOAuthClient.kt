package com.gymcheck.security.oauth.google

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.gymcheck.domain.user.SocialProvider
import com.gymcheck.exception.CustomException
import com.gymcheck.exception.ErrorCode
import com.gymcheck.security.oauth.OAuthClient
import com.gymcheck.security.oauth.OAuthUserInfo
import org.springframework.stereotype.Component

@Component
class GoogleOAuthClient(
    private val verifier: GoogleIdTokenVerifier,
) : OAuthClient {

    override val provider: SocialProvider = SocialProvider.GOOGLE

    /**
     * Flutter 앱이 전달한 Google idToken을 검증하고 사용자 정보를 반환한다.
     *
     * verifier.verify()는 audience, issuer, expiry를 모두 자동 검증한다.
     * null 반환은 검증 실패(위변조, 만료 등)를 의미한다.
     *
     * @param code OAuthClient 인터페이스 시그니처 유지 — 실제로는 Google idToken JWT
     */
    override fun fetchUserInfo(code: String): OAuthUserInfo {
        val idToken = try {
            verifier.verify(code)
        } catch (e: Exception) {
            throw CustomException(ErrorCode.UNAUTHORIZED, "Google idToken 검증에 실패했습니다.")
        } ?: throw CustomException(ErrorCode.UNAUTHORIZED, "유효하지 않은 Google idToken입니다.")

        val payload = idToken.payload
        return OAuthUserInfo(
            socialId = payload.subject,
            email = payload["email"] as? String,
            nickname = payload["name"] as? String,
        )
    }
}
