package com.gymcheck.security.jwt

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey
import org.springframework.stereotype.Component

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties,
) {
    fun createAccessToken(userId: Long): String = createToken(userId, jwtProperties.accessTokenExpiration)

    fun createRefreshToken(userId: Long): String = createToken(userId, jwtProperties.refreshTokenExpiration)

    fun validateToken(token: String): Boolean {
        return try {
            parseClaims(token)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getUserIdFromToken(token: String): Long {
        return parseClaims(token).subject.toLong()
    }

    fun getExpiration(token: String): LocalDateTime {
        return parseClaims(token).expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    fun getAuthentication(token: String): UsernamePasswordAuthenticationToken {
        return UsernamePasswordAuthenticationToken(
            UserPrincipal(getUserIdFromToken(token)),
            null,
            emptyList<GrantedAuthority>(),
        )
    }

    private fun createToken(userId: Long, expirationMillis: Long): String {
        val now = Instant.now()
        val expiration = now.plusMillis(expirationMillis)

        // subject에는 내부 userId만 넣는다. 사용자 권한/프로필 정보가 필요하면 DB에서 다시 조회한다.
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(userId.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(signingKey())
            .compact()
    }

    private fun parseClaims(token: String) =
        Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .payload

    private fun signingKey(): SecretKey {
        val secretBytes = jwtProperties.secret.toByteArray(Charsets.UTF_8)
        return Keys.hmacShaKeyFor(secretBytes)
    }
}
