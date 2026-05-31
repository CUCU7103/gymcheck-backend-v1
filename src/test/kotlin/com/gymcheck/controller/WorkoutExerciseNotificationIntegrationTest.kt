package com.gymcheck.controller

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.LocalDate

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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkoutExerciseNotificationIntegrationTest {

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
    @Autowired lateinit var objectMapper: ObjectMapper
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var exerciseTypeRepository: ExerciseTypeRepository
    @Autowired lateinit var workoutLogRepository: WorkoutLogRepository
    @Autowired lateinit var refreshTokenRepository: com.gymcheck.repository.RefreshTokenRepository
    @Autowired lateinit var fcmTokenRepository: FcmTokenRepository
    @Autowired lateinit var notificationSettingRepository: NotificationSettingRepository

    @BeforeEach
    fun cleanUp() {
        notificationSettingRepository.deleteAll()
        fcmTokenRepository.deleteAll()
        workoutLogRepository.deleteAll()
        exerciseTypeRepository.deleteAll()
        refreshTokenRepository.deleteAll()
        userRepository.deleteAll()
        googleWireMockServer.resetAll()
    }

    @AfterAll
    fun tearDown() {
        googleWireMockServer.stop()
    }

    @Test
    fun `exercise workout and notification flows work together`() {
        stubGoogleLogin()
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
        }.andReturn().response.contentAsByteArray.let {
            objectMapper.readValue(it, NotificationSettingsResponse::class.java)
        }
        assertThat(settings.enabled).isTrue()
        assertThat(settings.timezone).isEqualTo("Asia/Seoul")

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
            content = """{"code":"google-auth-code"}"""
        }.andExpect {
            status { isOk() }
        }.andReturn()

        return objectMapper.readValue(result.response.contentAsByteArray, TokenResponse::class.java)
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
