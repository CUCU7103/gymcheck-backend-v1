package com.gymcheck.controller.auth

import com.gymcheck.domain.user.SocialProvider
import com.gymcheck.dto.request.OAuthLoginRequest
import com.gymcheck.dto.request.RefreshTokenRequest
import com.gymcheck.dto.response.TokenResponse
import com.gymcheck.security.jwt.UserPrincipal
import com.gymcheck.service.auth.AuthService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService,
) {

    @PostMapping("/oauth/google")
    fun googleLogin(@Valid @RequestBody request: OAuthLoginRequest): TokenResponse {
        return authService.processLogin(SocialProvider.GOOGLE, request.code)
    }

    @PostMapping("/oauth/kakao")
    fun kakaoLogin(@Valid @RequestBody request: OAuthLoginRequest): TokenResponse {
        return authService.processLogin(SocialProvider.KAKAO, request.code)
    }

    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): TokenResponse {
        return authService.refreshAccessToken(request.refreshToken)
    }

    @DeleteMapping("/logout")
    fun logout(@AuthenticationPrincipal user: UserPrincipal): ResponseEntity<Void> {
        authService.logout(user.id)
        return ResponseEntity.noContent().build()
    }
}
