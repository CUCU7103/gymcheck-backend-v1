package com.gymcheck.dto.request

import jakarta.validation.constraints.NotNull
import java.time.LocalTime

data class UpdateNotificationSettingsRequest(
    @field:NotNull
    val enabled: Boolean,
    val notifyTime: LocalTime? = null,
    val timezone: String? = null,
)
