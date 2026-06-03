package com.gymcheck.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.gymcheck.dto.response.MonthlyCalendarResponse
import com.gymcheck.dto.response.ExerciseTypeResponse
import com.gymcheck.dto.response.StreakResponse
import com.gymcheck.dto.response.TokenResponse
import com.gymcheck.dto.response.StatisticsSummaryResponse
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.assertj.core.api.Assertions.assertThat
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
class StatsIntegrationTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var objectMapper: ObjectMapper

    @MockBean lateinit var googleIdTokenVerifier: GoogleIdTokenVerifier
    @MockBean lateinit var clock: Clock

    @Test
    fun `stats endpoints return streak calendar and summary`() {
        stubToday("2026-05-31T12:00:00Z")
        stubGoogleIdToken("google-id-token", "google-sub-1", "g@example.com", "Google User")
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
            jsonPath("$.isGoalAchievedToday") { value(true) }
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
            jsonPath("$.days[29].hasWorkout") { value(true) }
        }.andReturn().response.contentAsByteArray.let {
            objectMapper.readValue(it, MonthlyCalendarResponse::class.java)
        }
        assertThat(calendar.days.first { it.date.toString() == "2026-05-31" }.workoutCount).isEqualTo(1)

        val summary = mockMvc.get("/stats/summary") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.monthlyTotal") { value(2) }
            jsonPath("$.weeklyProgress.current") { value(2) }
            jsonPath("$.weeklyProgress.goal") { value(7) }
            jsonPath("$.exerciseTypeStats[0].exerciseType.name") { value("복싱") }
        }.andReturn().response.contentAsByteArray.let {
            objectMapper.readValue(it, StatisticsSummaryResponse::class.java)
        }
        assertThat(summary.totalWorkoutCount).isEqualTo(2)
        assertThat(summary.exerciseTypeCounts).isNotEmpty
        assertThat(summary.monthlyTotal).isEqualTo(2)
        assertThat(summary.weeklyProgress.current).isEqualTo(2)
        assertThat(summary.exerciseTypeStats).isNotEmpty
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

    private fun stubToday(instant: String) {
        `when`(clock.zone).thenReturn(ZoneId.of("Asia/Seoul"))
        `when`(clock.instant()).thenReturn(Instant.parse(instant))
    }
}
