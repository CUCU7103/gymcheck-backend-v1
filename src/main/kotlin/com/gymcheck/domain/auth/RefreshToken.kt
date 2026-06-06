package com.gymcheck.domain.auth

import com.gymcheck.domain.BaseEntity
import com.gymcheck.domain.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 로그인 상태를 연장하기 위한 리프레시 토큰.
 *
 * 현재 서비스 정책은 사용자당 하나의 토큰만 유지하며, 새 로그인 시 기존 토큰을 폐기한다.
 */
@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(nullable = false, unique = true, length = 512)
    var token: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,
) : BaseEntity()
