package com.gymcheck.security.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenExpiration: Long = 1_800_000,
    val refreshTokenExpiration: Long = 2_592_000_000,
)
