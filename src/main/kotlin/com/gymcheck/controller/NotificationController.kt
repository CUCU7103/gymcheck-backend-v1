package com.gymcheck.controller

import com.gymcheck.dto.request.DeleteFcmTokenRequest
import com.gymcheck.dto.request.RegisterFcmTokenRequest
import com.gymcheck.dto.request.UpdateNotificationSettingsRequest
import com.gymcheck.dto.response.NotificationSettingsResponse
import com.gymcheck.exception.ApiErrorResponse
import com.gymcheck.security.jwt.UserPrincipal
import com.gymcheck.service.NotificationService
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/notifications")
@Tag(name = "알림", description = "알림 설정과 FCM 토큰을 관리하는 엔드포인트")
class NotificationController(
    private val notificationService: NotificationService,
) {

    @Operation(
        summary = "알림 설정 조회",
        description = "현재 사용자의 알림 설정을 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = NotificationSettingsResponse::class))],
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
    @GetMapping("/settings")
    fun getSettings(
        @AuthenticationPrincipal user: UserPrincipal,
    ): NotificationSettingsResponse = notificationService.getSettings(user.id)

    @Operation(
        summary = "알림 설정 수정",
        description = "현재 사용자의 알림 설정을 수정합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "수정 성공",
                content = [Content(schema = Schema(implementation = NotificationSettingsResponse::class))],
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
    @PutMapping("/settings")
    fun updateSettings(
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody request: UpdateNotificationSettingsRequest,
    ): NotificationSettingsResponse = notificationService.updateSettings(user.id, request)

    @Operation(
        summary = "FCM 토큰 등록",
        description = "푸시 알림 수신을 위한 FCM 토큰을 등록합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "등록 성공",
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
    @PostMapping("/tokens")
    fun registerToken(
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody request: RegisterFcmTokenRequest,
    ): ResponseEntity<Void> {
        notificationService.registerFcmToken(user.id, request)
        return ResponseEntity.noContent().build()
    }

    @Operation(
        summary = "FCM 토큰 삭제",
        description = "등록된 FCM 토큰을 삭제합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "삭제 성공",
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
    @DeleteMapping("/tokens")
    fun deleteToken(
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody request: DeleteFcmTokenRequest,
    ): ResponseEntity<Void> {
        notificationService.deleteFcmToken(user.id, request.token)
        return ResponseEntity.noContent().build()
    }
}
