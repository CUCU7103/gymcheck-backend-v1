package com.gymcheck.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.gymcheck.dto.response.GoalResponse
import com.gymcheck.dto.response.TokenResponse
import com.gymcheck.dto.response.UserProfileResponse
import com.gymcheck.domain.user.GoalType
import com.gymcheck.repository.RefreshTokenRepository
import com.gymcheck.repository.UserGoalRepository
import com.gymcheck.repository.UserRepository
import com.gymcheck.repository.WorkoutLogRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.http.HttpHeaders
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
class UserControllerIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var userGoalRepository: UserGoalRepository
    @Autowired lateinit var refreshTokenRepository: RefreshTokenRepository
    @Autowired lateinit var workoutLogRepository: WorkoutLogRepository
    @Autowired lateinit var objectMapper: ObjectMapper

    @MockBean lateinit var googleIdTokenVerifier: GoogleIdTokenVerifier

    @BeforeEach
    fun setUp() {
        refreshTokenRepository.deleteAll()
        workoutLogRepository.deleteAll()
        userGoalRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `profile goal and account lifecycle works`() {
        stubGoogleIdToken("google-id-token", "google-sub-1", "g@example.com", "Google User")
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
            content = """{"idToken":"google-id-token"}"""
        }.andExpect {
            status { isOk() }
        }.andReturn()
        return objectMapper.readValue(result.response.contentAsString, TokenResponse::class.java)
    }

    private fun stubGoogleIdToken(idTokenString: String, sub: String, email: String, name: String) {
        val payload = mock(GoogleIdToken.Payload::class.java)
        `when`(payload.subject).thenReturn(sub)
        `when`(payload["email"]).thenReturn(email)
        `when`(payload["name"]).thenReturn(name)

        val idToken = mock(GoogleIdToken::class.java)
        `when`(idToken.payload).thenReturn(payload)

        `when`(googleIdTokenVerifier.verify(idTokenString)).thenReturn(idToken)
    }
}
