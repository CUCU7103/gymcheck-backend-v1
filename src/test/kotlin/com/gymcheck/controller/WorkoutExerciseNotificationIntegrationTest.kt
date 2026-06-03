package com.gymcheck.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.gymcheck.dto.response.ExerciseTypeResponse
import com.gymcheck.dto.response.NotificationSettingsResponse
import com.gymcheck.dto.response.TokenResponse
import com.gymcheck.dto.response.WorkoutLogResponse
import com.gymcheck.repository.ExerciseTypeRepository
import com.gymcheck.repository.FcmTokenRepository
import com.gymcheck.repository.NotificationSettingRepository
import com.gymcheck.repository.UserRepository
import com.gymcheck.repository.WorkoutLogRepository
import com.gymcheck.domain.user.SocialProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:gymcheck_workout;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.id.insert_returning_enabled=false",
    ],
)
@AutoConfigureMockMvc
class WorkoutExerciseNotificationIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var exerciseTypeRepository: ExerciseTypeRepository
    @Autowired lateinit var workoutLogRepository: WorkoutLogRepository
    @Autowired lateinit var refreshTokenRepository: com.gymcheck.repository.RefreshTokenRepository
    @Autowired lateinit var fcmTokenRepository: FcmTokenRepository
    @Autowired lateinit var notificationSettingRepository: NotificationSettingRepository

    @MockBean lateinit var googleIdTokenVerifier: GoogleIdTokenVerifier

    @BeforeEach
    fun cleanUp() {
        notificationSettingRepository.deleteAll()
        fcmTokenRepository.deleteAll()
        workoutLogRepository.deleteAll()
        exerciseTypeRepository.deleteAll()
        refreshTokenRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `exercise workout and notification flows work together`() {
        stubGoogleIdToken("google-id-token", "google-sub-1", "g@example.com", "Google User")
        val token = login().accessToken

        val createdExercise = mockMvc.post("/exercise-types") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"복싱"}"""
        }.andExpect {
            status { isCreated() }
        }.andReturn().response.contentAsByteArray.let { objectMapper.readValue(it, ExerciseTypeResponse::class.java) }

        mockMvc.post("/workout-logs") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"exerciseTypeId":${createdExercise.id},"logDate":"2026-05-31","memo":"AM"}"""
        }.andExpect {
            status { isCreated() }
        }
        mockMvc.post("/workout-logs") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"exerciseTypeId":${createdExercise.id},"logDate":"2026-05-31","memo":"PM"}"""
        }.andExpect {
            status { isCreated() }
        }

        val logs = mockMvc.get("/workout-logs") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            param("logDate", "2026-05-31")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].exerciseType.name") { value("복싱") }
        }.andReturn().response.contentAsByteArray.let {
            objectMapper.readValue(it, Array<WorkoutLogResponse>::class.java).toList()
        }
        assertThat(logs).hasSize(2)

        val exerciseTypes = mockMvc.get("/exercise-types") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsByteArray.let {
            objectMapper.readValue(it, Array<ExerciseTypeResponse>::class.java).toList()
        }
        assertThat(exerciseTypes.first().name).isEqualTo("복싱")
        assertThat(exerciseTypes.first().usageCount).isEqualTo(2)

        mockMvc.post("/notifications/tokens") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"token":"fcm-token-1"}"""
        }.andExpect {
            status { isNoContent() }
        }

        mockMvc.put("/notifications/settings") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"enabled":true,"notifyTime":"20:30:00","timezone":"Asia/Seoul"}"""
        }.andExpect {
            status { isOk() }
        }

        val settings = mockMvc.get("/notifications/settings") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.isEnabled") { value(true) }
            jsonPath("$.notificationHour") { value(20) }
            jsonPath("$.notificationMinute") { value(30) }
        }.andReturn().response.contentAsByteArray.let {
            objectMapper.readValue(it, NotificationSettingsResponse::class.java)
        }
        assertThat(settings.enabled).isTrue()
        assertThat(settings.isEnabled).isTrue()
        assertThat(settings.notificationHour).isEqualTo(20)
        assertThat(settings.notificationMinute).isEqualTo(30)
        assertThat(settings.timezone).isEqualTo("Asia/Seoul")

        mockMvc.put("/notifications/settings") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"isEnabled":false,"notificationHour":7,"notificationMinute":45}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.enabled") { value(false) }
            jsonPath("$.isEnabled") { value(false) }
            jsonPath("$.notifyTime") { value("07:45:00") }
            jsonPath("$.notificationHour") { value(7) }
            jsonPath("$.notificationMinute") { value(45) }
        }

        mockMvc.delete("/notifications/tokens") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"token":"fcm-token-1"}"""
        }.andExpect {
            status { isNoContent() }
        }

        assertThat(fcmTokenRepository.findByUserId(userRepository.findBySocialProviderAndSocialId(SocialProvider.GOOGLE, "google-sub-1")!!.id!!))
            .isEmpty()
    }

    private fun login(): TokenResponse {
        val result = mockMvc.post("/auth/oauth/google") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"idToken":"google-id-token"}"""
        }.andExpect {
            status { isOk() }
        }.andReturn()

        return objectMapper.readValue(result.response.contentAsByteArray, TokenResponse::class.java)
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
