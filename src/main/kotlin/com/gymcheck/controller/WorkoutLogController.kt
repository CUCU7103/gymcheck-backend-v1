package com.gymcheck.controller

import com.gymcheck.dto.request.CreateWorkoutLogRequest
import com.gymcheck.dto.response.WorkoutLogResponse
import com.gymcheck.exception.ApiErrorResponse
import com.gymcheck.security.jwt.UserPrincipal
import com.gymcheck.service.WorkoutLogService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.time.LocalDate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/workout-logs")
@Tag(name = "운동 기록", description = "운동 기록을 조회, 생성, 삭제하는 엔드포인트")
class WorkoutLogController(
    private val workoutLogService: WorkoutLogService,
) {

    @Operation(
        summary = "운동 기록 목록 조회",
        description = "특정 날짜의 운동 기록 목록을 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = WorkoutLogResponse::class))],
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
    @GetMapping
    fun getWorkoutLogs(
        @AuthenticationPrincipal user: UserPrincipal,
        @RequestParam logDate: LocalDate,
    ): List<WorkoutLogResponse> = workoutLogService.getWorkoutLogs(user.id, logDate)

    @Operation(
        summary = "운동 기록 생성",
        description = "새로운 운동 기록을 생성합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "생성 성공",
                content = [Content(schema = Schema(implementation = WorkoutLogResponse::class))],
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
                responseCode = "404",
                description = "대상을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 오류",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    @PostMapping
    fun createWorkoutLog(
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody request: CreateWorkoutLogRequest,
    ): ResponseEntity<WorkoutLogResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(workoutLogService.createWorkoutLog(user.id, request))
    }

    @Operation(
        summary = "운동 기록 삭제",
        description = "지정한 운동 기록을 삭제합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "삭제 성공",
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "대상을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 오류",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    @DeleteMapping("/{workoutLogId}")
    fun deleteWorkoutLog(
        @AuthenticationPrincipal user: UserPrincipal,
        @PathVariable workoutLogId: Long,
    ): ResponseEntity<Void> {
        workoutLogService.deleteWorkoutLog(user.id, workoutLogId)
        return ResponseEntity.noContent().build()
    }
}
