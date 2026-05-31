package com.gymcheck.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.gymcheck.dto.response.MonthlyCalendarResponse
import com.gymcheck.dto.response.ExerciseTypeResponse
import com.gymcheck.dto.response.StreakResponse
import com.gymcheck.dto.response.TokenResponse
import com.gymcheck.dto.response.StatisticsSummaryResponse
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:gymcheck_stats;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.id.insert_returning_enabled=false",
    ],
)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StatsIntegrationTest {

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

    @BeforeEach
    fun reset() {
        googleWireMockServer.resetAll()
    }

    @AfterAll
    fun tearDown() {
        googleWireMockServer.stop()
    }

    @Test
    fun `stats endpoints return streak calendar and summary`() {
        stubGoogleLogin()
        val token = login().accessToken

        val exerciseType = mockMvc.post("/exercise-types") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"복싱"}"""
        }.andExpect {
            status { isCreated() }
        }.andReturn().response.contentAsByteArray.let {
            objectMapper.readValue(it, ExerciseTypeResponse::class.java)
        }

        mockMvc.post("/workout-logs") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"exerciseTypeId":${exerciseType.id},"logDate":"2026-05-30"}"""
        }.andExpect {
            status { isCreated() }
        }
        mockMvc.post("/workout-logs") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"exerciseTypeId":${exerciseType.id},"logDate":"2026-05-31"}"""
        }.andExpect {
            status { isCreated() }
        }

        val streak = mockMvc.get("/stats/streak") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsByteArray.let {
            objectMapper.readValue(it, StreakResponse::class.java)
        }
        assertThat(streak.currentStreak).isEqualTo(2)

        val calendar = mockMvc.get("/stats/calendar") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            param("year", "2026")
            param("month", "5")
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsByteArray.let {
            objectMapper.readValue(it, MonthlyCalendarResponse::class.java)
        }
        assertThat(calendar.days.first { it.date.toString() == "2026-05-31" }.workoutCount).isEqualTo(1)

        val summary = mockMvc.get("/stats/summary") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            param("year", "2026")
            param("month", "5")
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsByteArray.let {
            objectMapper.readValue(it, StatisticsSummaryResponse::class.java)
        }
        assertThat(summary.totalWorkoutCount).isEqualTo(2)
        assertThat(summary.exerciseTypeCounts).isNotEmpty
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
