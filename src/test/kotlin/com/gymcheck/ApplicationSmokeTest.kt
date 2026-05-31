package com.gymcheck

import com.gymcheck.exception.CustomException
import com.gymcheck.exception.ErrorCode
import com.gymcheck.exception.GlobalExceptionHandler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.HttpStatus
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(controllers = [com.gymcheck.controller.HealthController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class ApplicationSmokeTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var globalExceptionHandler: GlobalExceptionHandler

    @Test
    fun `context loads`() {
        assertThat(mockMvc).isNotNull
    }

    @Test
    fun `health endpoint returns up`() {
        mockMvc.get("/health")
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("UP") }
            }
    }

    @Test
    fun `custom exception is mapped to api error response`() {
        val response = globalExceptionHandler.handleCustomException(
            CustomException(ErrorCode.BAD_REQUEST, "잘못된 요청입니다."),
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body).isNotNull
        assertThat(response.body!!.code).isEqualTo(ErrorCode.BAD_REQUEST.code)
        assertThat(response.body!!.message).isEqualTo("잘못된 요청입니다.")
        assertThat(response.body!!.timestamp).isNotNull
    }
}
