package com.gymcheck.domain.notification

import com.gymcheck.domain.BaseEntity
import com.gymcheck.domain.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalTime

/**
 * 사용자의 푸시 알림 수신 여부와 발송 시각을 저장하는 설정 엔티티.
 */
@Entity
@Table(name = "notification_settings")
class NotificationSetting(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    var user: User,

    @Column(nullable = false)
    var enabled: Boolean = true,

    @Column(name = "notify_time")
    var notifyTime: LocalTime? = null,

    @Column(length = 50)
    var timezone: String? = null,
) : BaseEntity()
