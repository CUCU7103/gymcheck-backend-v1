package com.gymcheck.service

import com.gymcheck.repository.NotificationSettingRepository
import java.time.Clock
import java.time.LocalTime
import java.time.ZoneId
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
     * 현재 UTC 기준으로 ±1분 범위의 notifyTime을 가진 설정을 DB에서 직접 필터링해 조회한다.
     * timezone은 개별 설정에서 확인하여 사용자 로컬 시간과 비교한다.
     */
    @Scheduled(cron = "0 * * * * *")
    fun sendScheduledNotifications() {
        val utcNow = LocalTime.now(Clock.systemUTC())
        // DB에서 UTC 기준 ±1분 범위로 1차 필터링 (timezone 미설정 사용자 대상)
        val from = utcNow.minusMinutes(1)
        val to = utcNow.plusMinutes(1)

        val settings = notificationSettingRepository.findAllEnabledBetween(from, to)
        settings.forEach { setting ->
            val notifyTime = setting.notifyTime ?: return@forEach
            val zoneId = runCatching { setting.timezone?.let { ZoneId.of(it) } }
                .getOrNull() ?: ZoneId.systemDefault()
            val localNow = LocalTime.now(Clock.system(zoneId))

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
        return diff < 60 || diff > 86340
    }
}
