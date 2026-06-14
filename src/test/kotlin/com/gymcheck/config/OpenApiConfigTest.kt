package com.gymcheck.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OpenApiConfigTest {

    @Test
    fun `open api requires bearer authentication by default`() {
        val openApi = OpenApiConfig().openApi()

        assertThat(openApi.security)
            .extracting<String> { it.keys.single() }
            .containsExactly("BearerAuth")
    }
}
