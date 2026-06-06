package com.gymcheck.repository

import com.gymcheck.domain.notification.NotificationSetting
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationSettingRepository : JpaRepository<NotificationSetting, Long> {
    fun findByUserId(userId: Long): NotificationSetting?
    fun findAllByEnabledTrue(): List<NotificationSetting>
}
