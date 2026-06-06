package com.gymcheck.service

import com.gymcheck.repository.NotificationSettingRepository
import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["firebase.enabled"], havingValue = "true", matchIfMissing = true)
class NotificationScheduler(
    private val notificationSettingRepository: NotificationSettingRepository,
    private val fcmService: FcmService,
    private val clock: Clock,
) {

    private val log = LoggerFactory.getLogger(NotificationScheduler::class.java)

    /**
     * 매분 0초에 실행.
     *
     * notifyTime은 사용자 로컬 시간으로 저장된다. 따라서 서버 시간이 아니라 각 설정의 timezone으로
     * 현재 시각을 변환한 뒤 발송 여부를 판단해야 한다.
     */
    @Scheduled(cron = "0 * * * * *")
    fun sendScheduledNotifications() {
        val settings = notificationSettingRepository.findAllByEnabledTrue()
        settings.forEach { setting ->
            val notifyTime = setting.notifyTime ?: return@forEach
            val zoneId = runCatching { setting.timezone?.let { ZoneId.of(it) } }
                .getOrNull() ?: ZoneId.systemDefault()
            val localNow = ZonedDateTime.now(clock).withZoneSameInstant(zoneId).toLocalTime()

            if (isWithinOneMinute(localNow, notifyTime)) {
                log.debug("알림 발송 대상: userId={}", setting.user.id)
                fcmService.sendNotification(
                    userId = setting.user.id!!,
                    title = "운동할 시간이에요!",
                    body = "오늘 운동 기록을 남겨보세요.",
                )
            }
        }
    }

    private fun isWithinOneMinute(now: LocalTime, target: LocalTime): Boolean {
        val diff = kotlin.math.abs(now.toSecondOfDay() - target.toSecondOfDay())
        // 자정 경계를 사이에 둔 23:59 ↔ 00:00 비교도 같은 1분 범위로 본다.
        return diff < 60 || diff > 86340
    }
}
