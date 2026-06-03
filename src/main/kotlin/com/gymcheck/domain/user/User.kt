package com.gymcheck.domain.user

import com.gymcheck.domain.BaseEntity
import com.gymcheck.domain.notification.FcmToken
import com.gymcheck.domain.notification.NotificationSetting
import com.gymcheck.domain.auth.RefreshToken
import com.gymcheck.domain.workout.WorkoutLog
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

/**
 * 소셜 로그인 기반으로 식별되는 사용자 엔티티.
 *
 * 사용자 프로필, 목표, 운동 기록, 알림 설정, 토큰 정보를 다른 도메인과 연결하는 중심 객체다.
 */
@Entity
@Table(name = "users")
class User(
    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", nullable = false, length = 20)
    var socialProvider: SocialProvider,

    @Column(name = "social_id", nullable = false)
    var socialId: String,

    @Column
    var email: String? = null,

    @Column(length = 100)
    var nickname: String? = null,
) : BaseEntity() {

    // 1:1 사용자 목표
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var userGoal: UserGoal? = null

    // 사용자의 운동 기록 히스토리
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var workoutLogs: MutableList<WorkoutLog> = mutableListOf()

    // 로그인 세션 유지를 위한 리프레시 토큰들
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var refreshTokens: MutableList<RefreshToken> = mutableListOf()

    // 푸시 알림 수신용 FCM 토큰들
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var fcmTokens: MutableList<FcmToken> = mutableListOf()

    // 1:1 알림 설정
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var notificationSetting: NotificationSetting? = null
}
