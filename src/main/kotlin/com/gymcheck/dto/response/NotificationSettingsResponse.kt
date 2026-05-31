package com.gymcheck.dto.response

import java.time.LocalTime

data class NotificationSettingsResponse(
    val enabled: Boolean,
    val notifyTime: LocalTime?,
    val timezone: String?,
)
