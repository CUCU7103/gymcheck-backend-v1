package com.gymcheck.service

import com.gymcheck.domain.notification.FcmToken
import com.gymcheck.domain.notification.NotificationSetting
import com.gymcheck.dto.request.RegisterFcmTokenRequest
import com.gymcheck.dto.request.UpdateNotificationSettingsRequest
import com.gymcheck.dto.response.NotificationSettingsResponse
import com.gymcheck.repository.FcmTokenRepository
import com.gymcheck.repository.NotificationSettingRepository
import java.time.LocalTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val fcmTokenRepository: FcmTokenRepository,
    private val notificationSettingRepository: NotificationSettingRepository,
    private val userFinder: UserFinder,
) {

    @Transactional(readOnly = true)
    fun getSettings(userId: Long): NotificationSettingsResponse {
        userFinder.findById(userId)
        val setting = notificationSettingRepository.findByUserId(userId)
            ?: return NotificationSettingsResponse(
                enabled = true,
                notifyTime = null,
                timezone = null,
            )

        return setting.toResponse()
    }

    @Transactional
    fun updateSettings(userId: Long, request: UpdateNotificationSettingsRequest): NotificationSettingsResponse {
        val user = userFinder.findById(userId)
        val setting = notificationSettingRepository.findByUserId(userId)
            ?: NotificationSetting(user = user)

        setting.enabled = request.enabled ?: request.isEnabled ?: setting.enabled
        setting.notifyTime = request.notifyTime ?: request.notificationTime() ?: setting.notifyTime
        setting.timezone = request.timezone ?: setting.timezone ?: "Asia/Seoul"

        return notificationSettingRepository.save(setting).toResponse()
    }

    @Transactional
    fun registerFcmToken(userId: Long, request: RegisterFcmTokenRequest) {
        val user = userFinder.findById(userId)
        if (fcmTokenRepository.findByUserIdAndToken(userId, request.token) != null) {
            return
        }

        fcmTokenRepository.save(
            FcmToken(
                user = user,
                token = request.token,
            ),
        )
    }

    @Transactional
    fun deleteFcmToken(userId: Long, token: String) {
        fcmTokenRepository.deleteByUserIdAndToken(userId, token)
    }

    private fun NotificationSetting.toResponse() = NotificationSettingsResponse(
        enabled = enabled,
        notifyTime = notifyTime,
        timezone = timezone,
    )

    private fun UpdateNotificationSettingsRequest.notificationTime(): LocalTime? {
        val hour = notificationHour ?: return null
        val minute = notificationMinute ?: return null
        return LocalTime.of(hour, minute)
    }
}
