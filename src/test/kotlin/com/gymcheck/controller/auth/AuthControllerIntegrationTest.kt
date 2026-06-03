package com.gymcheck.controller.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.gymcheck.domain.user.SocialProvider
import com.gymcheck.dto.response.TokenResponse
import com.gymcheck.repository.RefreshTokenRepository
import com.gymcheck.repository.UserRepository
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post as wireMockPost
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.post

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:gymcheck_auth;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.id.insert_returning_enabled=false",
    ],
)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthControllerIntegrationTest {

    companion object {
        private val kakaoWireMockServer = WireMockServer(options().dynamicPort())

        init {
            kakaoWireMockServer.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("oauth.kakao.token-uri") { "${kakaoWireMockServer.baseUrl()}/oauth/token" }
            registry.add("oauth.kakao.user-info-uri") { "${kakaoWireMockServer.baseUrl()}/v2/user/me" }
        }
    }

    @MockBean
    lateinit var googleIdTokenVerifier: GoogleIdTokenVerifier

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun cleanUp() {
        refreshTokenRepository.deleteAll()
        userRepository.deleteAll()
        kakaoWireMockServer.resetAll()
    }

    @AfterAll
    fun tearDown() {
        kakaoWireMockServer.stop()
    }

    @Test
    fun `google login creates user and stores refresh token`() {
        stubGoogleIdToken("google-id-token", "google-sub-1", "g@example.com", "Google User")

        val response = performGoogleLogin("google-id-token")

        assertThat(response.accessToken).isNotBlank
        assertThat(response.refreshToken).isNotBlank

        val user = userRepository.findBySocialProviderAndSocialId(SocialProvider.GOOGLE, "google-sub-1")
        assertThat(user).isNotNull
        assertThat(refreshTokenRepository.findByUserId(user!!.id!!)).hasSize(1)
    }

    @Test
    fun `refresh endpoint issues a new access token`() {
        stubGoogleIdToken("google-id-token", "google-sub-1", "g@example.com", "Google User")
        val loginResponse = performGoogleLogin("google-id-token")

        val result = mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"${loginResponse.refreshToken}"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { exists() }
            jsonPath("$.refreshToken") { value(loginResponse.refreshToken) }
        }.andReturn()

        val refreshed = objectMapper.readValue(result.response.contentAsString, TokenResponse::class.java)
        assertThat(refreshed.accessToken).isNotEqualTo(loginResponse.accessToken)
    }

    @Test
    fun `logout deletes refresh tokens for the authenticated user`() {
        stubGoogleIdToken("google-id-token", "google-sub-1", "g@example.com", "Google User")
        val loginResponse = performGoogleLogin("google-id-token")
        val user = userRepository.findBySocialProviderAndSocialId(SocialProvider.GOOGLE, "google-sub-1")
        assertThat(user).isNotNull

        mockMvc.delete("/auth/logout") {
            header(HttpHeaders.AUTHORIZATION, "Bearer ${loginResponse.accessToken}")
        }.andExpect {
            status { isNoContent() }
        }

        assertThat(refreshTokenRepository.findByUserId(user!!.id!!)).isEmpty()
    }

    @Test
    fun `kakao login creates user and stores refresh token`() {
        stubKakaoLogin()

        val response = performKakaoLogin("kakao-auth-code")

        assertThat(response.accessToken).isNotBlank
        assertThat(response.refreshToken).isNotBlank

        val user = userRepository.findBySocialProviderAndSocialId(SocialProvider.KAKAO, "42")
        assertThat(user).isNotNull
        assertThat(refreshTokenRepository.findByUserId(user!!.id!!)).hasSize(1)
    }

    private fun performGoogleLogin(idToken: String): TokenResponse {
        val result = mockMvc.post("/auth/oauth/google") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"idToken":"$idToken"}"""
        }.andExpect {
            status { isOk() }
        }.andReturn()

        return objectMapper.readValue(result.response.contentAsString, TokenResponse::class.java)
    }

    private fun performKakaoLogin(code: String): TokenResponse {
        val result = mockMvc.post("/auth/oauth/kakao") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"code":"$code"}"""
        }.andExpect {
            status { isOk() }
        }.andReturn()

        return objectMapper.readValue(result.response.contentAsString, TokenResponse::class.java)
    }

    private fun stubGoogleIdToken(idTokenString: String, sub: String, email: String, name: String) {
        // GoogleIdToken.Payload은 GenericJson 기반이라 직접 생성 가능
        val payload = mock(GoogleIdToken.Payload::class.java)
        `when`(payload.subject).thenReturn(sub)
        `when`(payload["email"]).thenReturn(email)
        `when`(payload["name"]).thenReturn(name)

        val idToken = mock(GoogleIdToken::class.java)
        `when`(idToken.payload).thenReturn(payload)

        `when`(googleIdTokenVerifier.verify(idTokenString)).thenReturn(idToken)
    }

    private fun stubKakaoLogin() {
        kakaoWireMockServer.stubFor(
            wireMockPost(urlEqualTo("/oauth/token"))
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
        kakaoWireMockServer.stubFor(
            get(urlEqualTo("/v2/user/me"))
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
    }
}
