package com.gymcheck.repository

import com.gymcheck.domain.notification.NotificationSetting
import java.time.LocalTime
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface NotificationSettingRepository : JpaRepository<NotificationSetting, Long> {
    fun findByUserId(userId: Long): NotificationSetting?
    fun findAllByEnabledTrue(): List<NotificationSetting>

    /**
     * 활성화된 알림 설정 중 notifyTime이 [from, to] 범위에 있는 설정만 조회.
     * 스케줄러가 매분 해당 범위의 사용자만 로드하도록 DB에서 필터링한다.
     */
    @Query("SELECT n FROM NotificationSetting n WHERE n.enabled = true AND n.notifyTime BETWEEN :from AND :to")
    fun findAllEnabledBetween(from: LocalTime, to: LocalTime): List<NotificationSetting>
}
