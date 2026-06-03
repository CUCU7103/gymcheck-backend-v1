package com.gymcheck.dto.request

import java.time.LocalTime

data class UpdateNotificationSettingsRequest(
    val enabled: Boolean? = null,
    val notifyTime: LocalTime? = null,
    val timezone: String? = null,
    val isEnabled: Boolean? = null,
    val notificationHour: Int? = null,
    val notificationMinute: Int? = null,
)
