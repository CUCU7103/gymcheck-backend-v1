package com.gymcheck.controller.auth

import com.gymcheck.domain.user.SocialProvider
import com.gymcheck.dto.request.GoogleLoginRequest
import com.gymcheck.dto.request.OAuthLoginRequest
import com.gymcheck.dto.request.RefreshTokenRequest
import com.gymcheck.dto.response.TokenResponse
import com.gymcheck.exception.ApiErrorResponse
import com.gymcheck.security.jwt.UserPrincipal
import com.gymcheck.service.auth.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "인증", description = "소셜 로그인, 토큰 재발급, 로그아웃을 처리하는 엔드포인트")
class AuthController(
    private val authService: AuthService,
) {

    @Operation(
        summary = "구글 로그인",
        description = "구글 OAuth ID 토큰을 사용해 로그인합니다.",
    )
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "로그인 성공",
                content = [Content(schema = Schema(implementation = TokenResponse::class))],
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
    @PostMapping("/oauth/google")
    fun googleLogin(@Valid @RequestBody request: GoogleLoginRequest): TokenResponse {
        return authService.processLogin(SocialProvider.GOOGLE, request.idToken)
    }

    @Operation(
        summary = "카카오 로그인",
        description = "카카오 OAuth 인가 코드를 사용해 로그인합니다.",
    )
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "로그인 성공",
                content = [Content(schema = Schema(implementation = TokenResponse::class))],
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
    @PostMapping("/oauth/kakao")
    fun kakaoLogin(@Valid @RequestBody request: OAuthLoginRequest): TokenResponse {
        return authService.processLogin(SocialProvider.KAKAO, request.code)
    }

    @Operation(
        summary = "액세스 토큰 재발급",
        description = "리프레시 토큰으로 새로운 액세스 토큰을 발급합니다.",
    )
    @SecurityRequirements
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "재발급 성공",
                content = [Content(schema = Schema(implementation = TokenResponse::class))],
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
    @PostMapping("/refresh")
    fun refreshToken(@Valid @RequestBody request: RefreshTokenRequest): TokenResponse {
        return authService.refreshAccessToken(request.refreshToken)
    }

    @Operation(
        summary = "로그아웃",
        description = "현재 사용자 세션의 로그아웃 처리를 수행합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "로그아웃 성공",
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
    @DeleteMapping("/logout")
    fun logout(@AuthenticationPrincipal user: UserPrincipal): ResponseEntity<Void> {
        authService.logout(user.id)
        return ResponseEntity.noContent().build()
    }
}
