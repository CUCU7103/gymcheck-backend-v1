package com.gymcheck.security.oauth.google

import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.WireMockServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class GoogleOAuthClientTest {

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

    private val properties = GoogleOAuthProperties(
        clientId = "client-id",
        clientSecret = "client-secret",
        redirectUri = "http://localhost/callback",
        tokenUri = "${wireMockServer.baseUrl()}/token",
        userInfoUri = "${wireMockServer.baseUrl()}/userinfo",
    )

    private val client = GoogleOAuthClient(properties)

    @Test
    fun `exchanges code for access token and loads user info`() {
        wireMockServer.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/token"))
                .willReturn(
                    okJson(
                        """
                        {
                          "access_token": "google-access",
                          "token_type": "Bearer",
                          "expires_in": 3600
                        }
                        """.trimIndent(),
                    ),
                ),
        )
        wireMockServer.stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/userinfo"))
                .willReturn(
                    okJson(
                        """
                        {
                          "sub": "google-sub-1",
                          "email": "g@example.com",
                          "name": "Google User",
                          "picture": "https://example.com/avatar.png"
                        }
                        """.trimIndent(),
                    ),
                ),
        )

        val token = client.exchangeCodeForToken("auth-code")
        val userInfo = client.getUserInfo("google-access")

        assertThat(token.accessToken).isEqualTo("google-access")
        assertThat(userInfo.sub).isEqualTo("google-sub-1")
        assertThat(userInfo.email).isEqualTo("g@example.com")
        wireMockServer.verify(postRequestedFor(urlEqualTo("/token")))
        wireMockServer.verify(getRequestedFor(urlEqualTo("/userinfo")))
    }
}
