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
