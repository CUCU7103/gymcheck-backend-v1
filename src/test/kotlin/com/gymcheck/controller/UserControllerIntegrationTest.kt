package com.gymcheck.controller

import com.gymcheck.dto.request.OAuthLoginRequest
import com.gymcheck.dto.request.UpdateGoalRequest
import com.gymcheck.dto.request.UpdateProfileRequest
import com.gymcheck.dto.response.GoalResponse
import com.gymcheck.dto.response.TokenResponse
import com.gymcheck.dto.response.UserProfileResponse
import com.gymcheck.domain.user.GoalType
import com.gymcheck.repository.RefreshTokenRepository
import com.gymcheck.repository.UserGoalRepository
import com.gymcheck.repository.UserRepository
import com.gymcheck.repository.WorkoutLogRepository
import com.gymcheck.security.jwt.JwtTokenProvider
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.http.HttpHeaders
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:gymcheck_users;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.id.insert_returning_enabled=false",
    ],
)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserControllerIntegrationTest {

    companion object {
        private val googleWireMockServer = WireMockServer(options().dynamicPort())

        init {
            googleWireMockServer.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("oauth.google.token-uri") { "${googleWireMockServer.baseUrl()}/token" }
            registry.add("oauth.google.user-info-uri") { "${googleWireMockServer.baseUrl()}/userinfo" }
        }
    }

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jwtTokenProvider: JwtTokenProvider
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var userGoalRepository: UserGoalRepository
    @Autowired lateinit var refreshTokenRepository: RefreshTokenRepository
    @Autowired lateinit var workoutLogRepository: WorkoutLogRepository
    @Autowired lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        refreshTokenRepository.deleteAll()
        workoutLogRepository.deleteAll()
        userGoalRepository.deleteAll()
        userRepository.deleteAll()
        googleWireMockServer.resetAll()
    }

    @AfterAll
    fun tearDown() {
        googleWireMockServer.stop()
    }

    @Test
    fun `profile goal and account lifecycle works`() {
        stubGoogleLogin()
        val login = login()
        val accessToken = login.accessToken

        val profile = mockMvc.get("/users/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsString.let { objectMapper.readValue(it, UserProfileResponse::class.java) }
        assertThat(profile.nickname).isEqualTo("Google User")

        val updatedProfile = mockMvc.put("/users/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            contentType = MediaType.APPLICATION_JSON
            content = """{"nickname":"new-nickname"}"""
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsString.let { objectMapper.readValue(it, UserProfileResponse::class.java) }
        assertThat(updatedProfile.nickname).isEqualTo("new-nickname")

        val updatedGoal = mockMvc.put("/users/me/goal") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
            contentType = MediaType.APPLICATION_JSON
            content = """{"goalType":"WEEKLY","weeklyCount":3}"""
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsString.let { objectMapper.readValue(it, GoalResponse::class.java) }
        assertThat(updatedGoal.goalType).isEqualTo(GoalType.WEEKLY)
        assertThat(updatedGoal.weeklyCount).isEqualTo(3)

        val fetchedGoal = mockMvc.get("/users/me/goal") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsString.let { objectMapper.readValue(it, GoalResponse::class.java) }
        assertThat(fetchedGoal.weeklyCount).isEqualTo(3)

        mockMvc.delete("/users/me") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
        }.andExpect {
            status { isNoContent() }
        }

        assertThat(userRepository.count()).isEqualTo(0)
        assertThat(userGoalRepository.count()).isEqualTo(0)
        assertThat(refreshTokenRepository.count()).isEqualTo(0)
        assertThat(workoutLogRepository.count()).isEqualTo(0)
    }

    private fun login(): TokenResponse {
        val result = mockMvc.post("/auth/oauth/google") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"code":"google-auth-code"}"""
        }.andExpect {
            status { isOk() }
        }.andReturn()
        return objectMapper.readValue(result.response.contentAsString, TokenResponse::class.java)
    }

    private fun stubGoogleLogin() {
        googleWireMockServer.stubFor(
            post(urlEqualTo("/token"))
                .willReturn(
                    okJson(
                        """
                        {"access_token":"google-access","token_type":"Bearer","expires_in":3600}
                        """.trimIndent(),
                    ),
                ),
        )
        googleWireMockServer.stubFor(
            get(urlEqualTo("/userinfo"))
                .willReturn(
                    okJson(
                        """
                        {"sub":"google-sub-1","email":"g@example.com","name":"Google User"}
                        """.trimIndent(),
                    ),
                ),
        )
    }
}
