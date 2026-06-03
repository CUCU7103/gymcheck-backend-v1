package com.gymcheck.controller

import com.gymcheck.dto.request.UpdateGoalRequest
import com.gymcheck.dto.request.UpdateProfileRequest
import com.gymcheck.dto.response.GoalResponse
import com.gymcheck.dto.response.UserProfileResponse
import com.gymcheck.exception.ApiErrorResponse
import com.gymcheck.security.jwt.UserPrincipal
import com.gymcheck.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
@Tag(name = "사용자", description = "내 프로필과 목표를 조회/수정하고 계정을 관리하는 엔드포인트")
class UserController(
    private val userService: UserService,
) {

    @Operation(
        summary = "내 프로필 조회",
        description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = UserProfileResponse::class))],
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
    @GetMapping("/me")
    fun getMyProfile(@AuthenticationPrincipal user: UserPrincipal): UserProfileResponse =
        userService.getProfile(user.id)

    @Operation(
        summary = "내 프로필 수정",
        description = "현재 로그인한 사용자의 프로필 정보를 수정합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "수정 성공",
                content = [Content(schema = Schema(implementation = UserProfileResponse::class))],
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
    @PutMapping("/me")
    fun updateMyProfile(
        @AuthenticationPrincipal user: UserPrincipal,
        @RequestBody @Valid request: UpdateProfileRequest,
    ): UserProfileResponse = userService.updateProfile(user.id, request)

    @Operation(
        summary = "내 목표 조회",
        description = "현재 로그인한 사용자의 운동 목표 정보를 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = GoalResponse::class))],
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
    @GetMapping("/me/goal")
    fun getMyGoal(@AuthenticationPrincipal user: UserPrincipal): GoalResponse =
        userService.getGoal(user.id)

    @Operation(
        summary = "내 목표 수정",
        description = "현재 로그인한 사용자의 운동 목표 정보를 수정합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "수정 성공",
                content = [Content(schema = Schema(implementation = GoalResponse::class))],
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
    @PutMapping("/me/goal")
    fun updateMyGoal(
        @AuthenticationPrincipal user: UserPrincipal,
        @RequestBody @Valid request: UpdateGoalRequest,
    ): GoalResponse = userService.updateGoal(user.id, request)

    @Operation(
        summary = "회원 탈퇴",
        description = "현재 로그인한 사용자의 계정을 삭제합니다.",
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
                responseCode = "500",
                description = "서버 오류",
                content = [Content(schema = Schema(implementation = ApiErrorResponse::class))],
            ),
        ],
    )
    @DeleteMapping("/me")
    fun deleteAccount(@AuthenticationPrincipal user: UserPrincipal): ResponseEntity<Unit> {
        userService.deleteAccount(user.id)
        return ResponseEntity.noContent().build()
    }
}
