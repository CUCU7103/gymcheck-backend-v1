package com.gymcheck.config

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.gymcheck.security.oauth.google.GoogleOAuthProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GoogleAuthConfig(
    private val properties: GoogleOAuthProperties,
) {
    @Bean
    fun googleIdTokenVerifier(): GoogleIdTokenVerifier =
        GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
            // audience 검증: Flutter serverClientId와 동일한 웹 클라이언트 ID여야 한다
            .setAudience(listOf(properties.clientId))
            .build()
}
