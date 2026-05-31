package com.gymcheck.security.oauth.kakao

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.gymcheck.domain.user.SocialProvider
import com.gymcheck.exception.CustomException
import com.gymcheck.exception.ErrorCode
import com.gymcheck.security.oauth.OAuthClient
import com.gymcheck.security.oauth.OAuthUserInfo
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClient

@Component
class KakaoOAuthClient(
    private val properties: KakaoOAuthProperties,
) : OAuthClient {

    private val restClient: RestClient = RestClient.create()

    override val provider: SocialProvider = SocialProvider.KAKAO

    /**
     * 인가 코드 → Kakao 액세스 토큰 → 사용자 정보 조회 후 공통 OAuthUserInfo 반환.
     */
    override fun fetchUserInfo(code: String): OAuthUserInfo {
        val kakaoToken = exchangeCodeForToken(code)
        val userInfo = getUserInfo(kakaoToken.accessToken)
        return OAuthUserInfo(
            socialId = userInfo.id,
            email = userInfo.kakaoAccount?.email,
            nickname = userInfo.kakaoAccount?.profile?.nickname,
        )
    }

    fun exchangeCodeForToken(code: String): KakaoTokenResponse {
        return try {
            restClient.post()
                .uri(properties.tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(buildTokenRequest(code))
                .retrieve()
                .body(KakaoTokenResponse::class.java)
                ?: throw CustomException(ErrorCode.UNAUTHORIZED, "Kakao 토큰 응답이 비어 있습니다.")
        } catch (exception: Exception) {
            throw CustomException(ErrorCode.UNAUTHORIZED, "Kakao 토큰 교환에 실패했습니다.")
        }
    }

    fun getUserInfo(accessToken: String): KakaoUserInfo {
        return try {
            restClient.get()
                .uri(properties.userInfoUri)
                .headers { headers -> headers.setBearerAuth(accessToken) }
                .retrieve()
                .body(KakaoUserInfo::class.java)
                ?: throw CustomException(ErrorCode.UNAUTHORIZED, "Kakao 사용자 정보 응답이 비어 있습니다.")
        } catch (exception: Exception) {
            throw CustomException(ErrorCode.UNAUTHORIZED, "Kakao 사용자 정보 조회에 실패했습니다.")
        }
    }

    private fun buildTokenRequest(code: String): MultiValueMap<String, String> {
        return LinkedMultiValueMap<String, String>().apply {
            add("code", code)
            add("client_id", properties.clientId)
            add("client_secret", properties.clientSecret)
            add("redirect_uri", properties.redirectUri)
            add("grant_type", "authorization_code")
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class KakaoTokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("refresh_token")
    val refreshToken: String? = null,
    @JsonProperty("token_type")
    val tokenType: String? = null,
    @JsonProperty("expires_in")
    val expiresIn: Long? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KakaoUserInfo(
    @JsonProperty("id")
    val id: String,
    @JsonProperty("kakao_account")
    val kakaoAccount: KakaoAccount? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KakaoAccount(
    @JsonProperty("email")
    val email: String? = null,
    @JsonProperty("profile")
    val profile: KakaoProfile? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class KakaoProfile(
    @JsonProperty("nickname")
    val nickname: String? = null,
)
