package com.gymcheck.repository

import com.gymcheck.domain.user.SocialProvider
import com.gymcheck.domain.user.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findBySocialProviderAndSocialId(
        socialProvider: SocialProvider,
        socialId: String,
    ): User?
}
