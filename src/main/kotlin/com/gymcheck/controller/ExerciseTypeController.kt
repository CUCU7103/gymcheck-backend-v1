package com.gymcheck.controller

import com.gymcheck.dto.request.CreateExerciseTypeRequest
import com.gymcheck.dto.request.UpdateExerciseTypeRequest
import com.gymcheck.dto.response.ExerciseTypeResponse
import com.gymcheck.exception.ApiErrorResponse
import com.gymcheck.security.jwt.UserPrincipal
import com.gymcheck.service.ExerciseTypeService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/exercise-types")
@Tag(name = "운동 종류", description = "기본 운동과 커스텀 운동 종류를 관리하는 엔드포인트")
class ExerciseTypeController(
    private val exerciseTypeService: ExerciseTypeService,
) {

    @Operation(
        summary = "운동 종류 목록 조회",
        description = "사용자에게 노출되는 운동 종류 목록을 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [
                    Content(
                        schema = Schema(implementation = ExerciseTypeResponse::class),
                    ),
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
    @GetMapping
    fun getExerciseTypes(
        @AuthenticationPrincipal user: UserPrincipal,
    ): List<ExerciseTypeResponse> = exerciseTypeService.getExerciseTypes(user.id)

    @Operation(
        summary = "커스텀 운동 종류 생성",
        description = "사용자 정의 운동 종류를 새로 등록합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "생성 성공",
                content = [Content(schema = Schema(implementation = ExerciseTypeResponse::class))],
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
                responseCode = "409",
                description = "중복 데이터",
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
    fun createExerciseType(
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody request: CreateExerciseTypeRequest,
    ): ResponseEntity<ExerciseTypeResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(exerciseTypeService.createCustomExerciseType(user.id, request))
    }

    @Operation(
        summary = "커스텀 운동 종류 수정",
        description = "기존 사용자 정의 운동 종류의 이름이나 정보를 수정합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "수정 성공",
                content = [Content(schema = Schema(implementation = ExerciseTypeResponse::class))],
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
                responseCode = "403",
                description = "권한 없음",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "404",
                description = "대상을 찾을 수 없음",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "409",
                description = "중복 데이터",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 오류",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    @PutMapping("/{exerciseTypeId}")
    fun updateExerciseType(
        @AuthenticationPrincipal user: UserPrincipal,
        @PathVariable exerciseTypeId: Long,
        @Valid @RequestBody request: UpdateExerciseTypeRequest,
    ): ExerciseTypeResponse = exerciseTypeService.updateCustomExerciseType(user.id, exerciseTypeId, request)

    @Operation(
        summary = "커스텀 운동 종류 삭제",
        description = "사용자 정의 운동 종류를 삭제합니다.",
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
                responseCode = "403",
                description = "권한 없음",
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
    @DeleteMapping("/{exerciseTypeId}")
    fun deleteExerciseType(
        @AuthenticationPrincipal user: UserPrincipal,
        @PathVariable exerciseTypeId: Long,
    ): ResponseEntity<Void> {
        exerciseTypeService.deleteCustomExerciseType(user.id, exerciseTypeId)
        return ResponseEntity.noContent().build()
    }
}
