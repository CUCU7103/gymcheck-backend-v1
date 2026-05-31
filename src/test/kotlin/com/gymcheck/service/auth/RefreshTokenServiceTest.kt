package com.gymcheck.service.auth

import com.gymcheck.domain.user.SocialProvider
import com.gymcheck.domain.user.User
import com.gymcheck.repository.RefreshTokenRepository
import com.gymcheck.repository.UserRepository
import com.gymcheck.security.jwt.JwtTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:gymcheck_refresh;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.id.insert_returning_enabled=false",
    ],
)
@Transactional
class RefreshTokenServiceTest {

    @Autowired
    lateinit var refreshTokenService: RefreshTokenService

    @Autowired
    lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var jwtTokenProvider: JwtTokenProvider

    @BeforeEach
    fun setUp() {
        refreshTokenRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `saveRefreshToken replaces existing token for same user`() {
        val user = userRepository.save(
            User(
                socialProvider = SocialProvider.GOOGLE,
                socialId = "google-1",
            ),
        )

        val firstToken = jwtTokenProvider.createRefreshToken(user.id!!)
        val secondToken = jwtTokenProvider.createRefreshToken(user.id!!)

        refreshTokenService.saveRefreshToken(user.id!!, firstToken)
        refreshTokenService.saveRefreshToken(user.id!!, secondToken)

        val tokens = refreshTokenRepository.findByUserId(user.id!!)

        assertThat(tokens).hasSize(1)
        assertThat(tokens.single().token).isEqualTo(secondToken)
        assertThat(refreshTokenRepository.findByToken(firstToken)).isNull()
    }

    @Test
    fun `validateAndGetUserId returns user id for stored token`() {
        val user = userRepository.save(
            User(
                socialProvider = SocialProvider.KAKAO,
                socialId = "kakao-1",
            ),
        )

        val token = jwtTokenProvider.createRefreshToken(user.id!!)
        refreshTokenService.saveRefreshToken(user.id!!, token)

        assertThat(refreshTokenService.validateAndGetUserId(token)).isEqualTo(user.id!!)
    }
}
