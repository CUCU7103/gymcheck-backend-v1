package com.gymcheck.domain.user

import com.gymcheck.domain.BaseEntity
import com.gymcheck.domain.notification.FcmToken
import com.gymcheck.domain.notification.NotificationSetting
import com.gymcheck.domain.workout.RefreshToken
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

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var userGoal: UserGoal? = null

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var workoutLogs: MutableList<WorkoutLog> = mutableListOf()

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var refreshTokens: MutableList<RefreshToken> = mutableListOf()

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var fcmTokens: MutableList<FcmToken> = mutableListOf()

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var notificationSetting: NotificationSetting? = null
}
