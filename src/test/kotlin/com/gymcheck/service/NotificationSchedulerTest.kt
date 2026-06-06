package com.gymcheck.service

import com.gymcheck.domain.notification.NotificationSetting
import com.gymcheck.domain.user.SocialProvider
import com.gymcheck.domain.user.User
import com.gymcheck.repository.NotificationSettingRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils

class NotificationSchedulerTest {

    private val notificationSettingRepository = mock(NotificationSettingRepository::class.java)
    private val fcmService = mock(FcmService::class.java)

    @Test
    fun `sends notification when user local time matches notify time`() {
        val clock = Clock.fixed(Instant.parse("2026-06-03T00:00:00Z"), ZoneOffset.UTC)
        val user = user(id = 1L)
        val setting = NotificationSetting(
            user = user,
            enabled = true,
            notifyTime = LocalTime.of(9, 0),
            timezone = "Asia/Seoul",
        )
        `when`(notificationSettingRepository.findAllByEnabledTrue()).thenReturn(listOf(setting))

        NotificationScheduler(notificationSettingRepository, fcmService, clock).sendScheduledNotifications()

        verify(fcmService).sendNotification(
            userId = 1L,
            title = "운동할 시간이에요!",
            body = "오늘 운동 기록을 남겨보세요.",
        )
    }

    @Test
    fun `does not send notification when user local time is outside notify window`() {
        val clock = Clock.fixed(Instant.parse("2026-06-03T00:00:00Z"), ZoneOffset.UTC)
        val user = user(id = 2L)
        val setting = NotificationSetting(
            user = user,
            enabled = true,
            notifyTime = LocalTime.of(10, 0),
            timezone = "Asia/Seoul",
        )
        `when`(notificationSettingRepository.findAllByEnabledTrue()).thenReturn(listOf(setting))

        NotificationScheduler(notificationSettingRepository, fcmService, clock).sendScheduledNotifications()

        verify(fcmService, never()).sendNotification(
            userId = 2L,
            title = "운동할 시간이에요!",
            body = "오늘 운동 기록을 남겨보세요.",
        )
    }

    private fun user(id: Long): User {
        val user = User(
            socialProvider = SocialProvider.GOOGLE,
            socialId = "google-$id",
            email = "user$id@example.com",
            nickname = "User $id",
        )
        ReflectionTestUtils.setField(user, "id", id)
        return user
    }
}
