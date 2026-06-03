package com.gymcheck.security.oauth.google

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.gymcheck.exception.CustomException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class GoogleOAuthClientTest {

    private val verifier = mock(GoogleIdTokenVerifier::class.java)
    private val client = GoogleOAuthClient(verifier)

    @Test
    fun `유효한 idToken으로 사용자 정보를 반환한다`() {
        val payload = mock(GoogleIdToken.Payload::class.java)
        `when`(payload.subject).thenReturn("google-sub-1")
        `when`(payload["email"]).thenReturn("g@example.com")
        `when`(payload["name"]).thenReturn("Google User")

        val idToken = mock(GoogleIdToken::class.java)
        `when`(idToken.payload).thenReturn(payload)
        `when`(verifier.verify("valid-id-token")).thenReturn(idToken)

        val result = client.fetchUserInfo("valid-id-token")

        assertThat(result.socialId).isEqualTo("google-sub-1")
        assertThat(result.email).isEqualTo("g@example.com")
        assertThat(result.nickname).isEqualTo("Google User")
    }

    @Test
    fun `verifier가 null을 반환하면 예외를 던진다`() {
        `when`(verifier.verify("invalid-id-token")).thenReturn(null)

        assertThatThrownBy { client.fetchUserInfo("invalid-id-token") }
            .isInstanceOf(CustomException::class.java)
            .hasMessageContaining("유효하지 않은 Google idToken")
    }

    @Test
    fun `verifier가 예외를 던지면 CustomException으로 감싸 던진다`() {
        `when`(verifier.verify("bad-token")).thenThrow(RuntimeException("network error"))

        assertThatThrownBy { client.fetchUserInfo("bad-token") }
            .isInstanceOf(CustomException::class.java)
            .hasMessageContaining("Google idToken 검증에 실패했습니다")
    }
}
