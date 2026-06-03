package com.gymcheck.controller

import com.gymcheck.dto.response.MonthlyCalendarResponse
import com.gymcheck.dto.response.StreakResponse
import com.gymcheck.dto.response.StatisticsSummaryResponse
import com.gymcheck.exception.ApiErrorResponse
import com.gymcheck.security.jwt.UserPrincipal
import com.gymcheck.service.StreakService
import com.gymcheck.service.StatisticsService
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import java.time.Clock
import java.time.LocalDate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.security.core.annotation.AuthenticationPrincipal

@RestController
@RequestMapping("/stats")
@Tag(name = "통계", description = "운동 기록 기반의 통계와 캘린더 정보를 조회하는 엔드포인트")
class StatsController(
    private val streakService: StreakService,
    private val statisticsService: StatisticsService,
    private val clock: Clock,
) {

    @Operation(
        summary = "연속 운동 일수 조회",
        description = "현재 사용자의 운동 연속 기록(streak)을 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [
                    Content(schema = Schema(implementation = StreakResponse::class)),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 오류",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    @GetMapping("/streak")
    fun getStreak(
        @AuthenticationPrincipal user: UserPrincipal,
    ): StreakResponse = streakService.getStreak(user.id)

    @Operation(
        summary = "월간 캘린더 조회",
        description = "지정한 연/월 기준으로 운동 기록이 표시된 월간 캘린더를 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [
                    Content(schema = Schema(implementation = MonthlyCalendarResponse::class)),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청값 오류",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 오류",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    @GetMapping("/calendar")
    fun getCalendar(
        @AuthenticationPrincipal user: UserPrincipal,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) month: Int?,
    ): MonthlyCalendarResponse {
        val targetMonth = targetMonth(year, month)
        return statisticsService.getMonthlyCalendar(user.id, targetMonth.year, targetMonth.monthValue)
    }

    @Operation(
        summary = "월간 운동 요약 조회",
        description = "지정한 연/월 기준으로 운동량 요약 통계를 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [
                    Content(schema = Schema(implementation = StatisticsSummaryResponse::class)),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청값 오류",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 오류",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    @GetMapping("/summary")
    fun getSummary(
        @AuthenticationPrincipal user: UserPrincipal,
        @RequestParam(required = false) year: Int?,
        @RequestParam(required = false) month: Int?,
    ): StatisticsSummaryResponse {
        val targetMonth = targetMonth(year, month)
        return statisticsService.getSummary(user.id, targetMonth.year, targetMonth.monthValue)
    }

    private fun targetMonth(year: Int?, month: Int?): LocalDate {
        val today = LocalDate.now(clock)
        return LocalDate.of(year ?: today.year, month ?: today.monthValue, 1)
    }
}
