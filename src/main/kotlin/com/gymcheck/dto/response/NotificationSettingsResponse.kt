package com.gymcheck.dto.response

import java.time.LocalTime

data class NotificationSettingsResponse(
    val enabled: Boolean,
    val notifyTime: LocalTime?,
    val timezone: String?,
    val isEnabled: Boolean = enabled,
    val notificationHour: Int = notifyTime?.hour ?: 20,
    val notificationMinute: Int = notifyTime?.minute ?: 0,
)
