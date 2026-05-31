package com.gymcheck.controller

import com.gymcheck.dto.request.DeleteFcmTokenRequest
import com.gymcheck.dto.request.RegisterFcmTokenRequest
import com.gymcheck.dto.request.UpdateNotificationSettingsRequest
import com.gymcheck.dto.response.NotificationSettingsResponse
import com.gymcheck.security.jwt.UserPrincipal
import com.gymcheck.service.NotificationService
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
class NotificationController(
    private val notificationService: NotificationService,
) {

    @GetMapping("/settings")
    fun getSettings(
        @AuthenticationPrincipal user: UserPrincipal,
    ): NotificationSettingsResponse = notificationService.getSettings(user.id)

    @PutMapping("/settings")
    fun updateSettings(
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody request: UpdateNotificationSettingsRequest,
    ): NotificationSettingsResponse = notificationService.updateSettings(user.id, request)

    @PostMapping("/tokens")
    fun registerToken(
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody request: RegisterFcmTokenRequest,
    ): ResponseEntity<Void> {
        notificationService.registerFcmToken(user.id, request)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/tokens")
    fun deleteToken(
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody request: DeleteFcmTokenRequest,
    ): ResponseEntity<Void> {
        notificationService.deleteFcmToken(user.id, request.token)
        return ResponseEntity.noContent().build()
    }
}
