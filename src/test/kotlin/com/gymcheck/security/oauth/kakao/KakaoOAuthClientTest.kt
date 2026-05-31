package com.gymcheck.security.oauth.kakao

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class KakaoOAuthClientTest {

    companion object {
        private val wireMockServer = WireMockServer(options().dynamicPort())

        @JvmStatic
        @BeforeAll
        fun startServer() {
            wireMockServer.start()
        }

        @JvmStatic
        @AfterAll
        fun stopServer() {
            wireMockServer.stop()
        }
    }

    private val properties = KakaoOAuthProperties(
        clientId = "client-id",
        clientSecret = "client-secret",
        redirectUri = "http://localhost/callback",
        tokenUri = "${wireMockServer.baseUrl()}/oauth/token",
        userInfoUri = "${wireMockServer.baseUrl()}/v2/user/me",
    )

    private val client = KakaoOAuthClient(properties)

    @Test
    fun `exchanges code for access token and loads user info`() {
        wireMockServer.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/oauth/token"))
                .willReturn(
                    okJson(
                        """
                        {
                          "access_token": "kakao-access",
                          "token_type": "bearer",
                          "refresh_token": "kakao-refresh",
                          "expires_in": 7200
                        }
                        """.trimIndent(),
                    ),
                ),
        )
        wireMockServer.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/v2/user/me"))
                .willReturn(
                    okJson(
                        """
                        {
                          "id": 42,
                          "kakao_account": {
                            "email": "k@example.com",
                            "profile": {
                              "nickname": "Kakao User"
                            }
                          }
                        }
                        """.trimIndent(),
                    ),
                ),
        )

        val token = client.exchangeCodeForToken("auth-code")
        val userInfo = client.getUserInfo("kakao-access")

        assertThat(token.accessToken).isEqualTo("kakao-access")
        assertThat(userInfo.id).isEqualTo("42")
        assertThat(userInfo.kakaoAccount?.email).isEqualTo("k@example.com")
        wireMockServer.verify(postRequestedFor(urlEqualTo("/oauth/token")))
        wireMockServer.verify(getRequestedFor(urlEqualTo("/v2/user/me")))
    }
}
