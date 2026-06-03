package com.gymcheck.controller

import com.gymcheck.config.SecurityConfig
import com.gymcheck.domain.user.SocialProvider
import com.gymcheck.security.jwt.JwtTokenProvider
import com.gymcheck.security.jwt.UserPrincipal
import com.gymcheck.service.WorkoutLogService
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(WorkoutLogController::class)
@AutoConfigureMockMvc
@Import(SecurityConfig::class)
class WorkoutLogControllerTest {

    @Autowired lateinit var mockMvc: MockMvc

    @MockBean lateinit var workoutLogService: WorkoutLogService
    @MockBean lateinit var clock: Clock
    @MockBean lateinit var jwtTokenProvider: JwtTokenProvider

    @Test
    fun `getWorkoutLogs accepts date alias`() {
        val principal = UserPrincipal(id = 42, socialProvider = SocialProvider.GOOGLE)
        val expectedDate = LocalDate.of(2026, 5, 31)

        `when`(workoutLogService.getWorkoutLogs(principal.id, expectedDate)).thenReturn(emptyList())

        mockMvc.get("/workout-logs") {
            param("date", "2026-05-31")
            with(authentication(UsernamePasswordAuthenticationToken(principal, "n/a", AuthorityUtils.NO_AUTHORITIES)))
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
        }

        verify(workoutLogService).getWorkoutLogs(principal.id, expectedDate)
    }

    @Test
    fun `getWorkoutLogs uses today when date is omitted`() {
        val principal = UserPrincipal(id = 42, socialProvider = SocialProvider.GOOGLE)
        val zoneId = ZoneId.of("Asia/Seoul")
        val expectedDate = LocalDate.of(2026, 6, 3)

        `when`(clock.zone).thenReturn(zoneId)
        `when`(clock.instant()).thenReturn(Instant.parse("2026-06-03T09:00:00Z"))
        `when`(workoutLogService.getWorkoutLogs(principal.id, expectedDate)).thenReturn(emptyList())

        mockMvc.get("/workout-logs") {
            with(authentication(UsernamePasswordAuthenticationToken(principal, "n/a", AuthorityUtils.NO_AUTHORITIES)))
        }.andExpect {
            status { isOk() }
            content { contentType(MediaType.APPLICATION_JSON) }
        }

        verify(workoutLogService).getWorkoutLogs(principal.id, expectedDate)
    }
}
