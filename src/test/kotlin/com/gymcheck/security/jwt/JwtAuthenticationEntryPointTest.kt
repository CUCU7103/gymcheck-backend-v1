package com.gymcheck.security.jwt

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.charset.StandardCharsets
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.BadCredentialsException

class JwtAuthenticationEntryPointTest {

    @Test
    fun `unauthorized response uses utf8 json`() {
        val response = MockHttpServletResponse()

        JwtAuthenticationEntryPoint(jacksonObjectMapper().findAndRegisterModules()).commence(
            MockHttpServletRequest(),
            response,
            BadCredentialsException("unauthorized"),
        )

        assertThat(response.characterEncoding).isEqualTo(StandardCharsets.UTF_8.name())
        assertThat(response.contentType).isEqualTo("application/json;charset=UTF-8")
        assertThat(response.contentAsString).contains("인증이 필요합니다.")
    }
}
