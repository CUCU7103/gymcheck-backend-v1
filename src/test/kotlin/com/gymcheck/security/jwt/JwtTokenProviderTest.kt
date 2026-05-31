package com.gymcheck.security.jwt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JwtTokenProviderTest {

    private val properties = JwtProperties(
        secret = "0123456789012345678901234567890123456789012345678901234567890123",
        accessTokenExpiration = 1_000,
        refreshTokenExpiration = 2_000,
    )

    private val provider = JwtTokenProvider(properties)

    @Test
    fun `creates and validates access token`() {
        val token = provider.createAccessToken(123L)

        assertThat(token).isNotBlank
        assertThat(provider.validateToken(token)).isTrue()
        assertThat(provider.getUserIdFromToken(token)).isEqualTo(123L)
    }

    @Test
    fun `creates refresh token with longer expiration than access token`() {
        val accessToken = provider.createAccessToken(123L)
        val refreshToken = provider.createRefreshToken(123L)

        assertThat(provider.validateToken(refreshToken)).isTrue()
        assertThat(provider.getExpiration(refreshToken))
            .isAfter(provider.getExpiration(accessToken))
    }
}
