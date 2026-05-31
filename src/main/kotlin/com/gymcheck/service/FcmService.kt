package com.gymcheck.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import com.gymcheck.domain.notification.FcmToken
import com.gymcheck.repository.FcmTokenRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["firebase.enabled"], havingValue = "true", matchIfMissing = true)
class FcmService(
    private val firebaseMessaging: FirebaseMessaging,
    private val fcmTokenRepository: FcmTokenRepository,
) {

    private val log = LoggerFactory.getLogger(FcmService::class.java)

    /**
     * 특정 사용자의 모든 FCM 토큰으로 푸시 알림 발송.
     * 만료/무효 토큰은 [handleInvalidToken]을 통해 자동 삭제한다.
     */
    fun sendNotification(userId: Long, title: String, body: String) {
        val tokens = fcmTokenRepository.findByUserId(userId)
        if (tokens.isEmpty()) return

        tokens.forEach { fcmToken ->
            sendToToken(fcmToken, userId, title, body)
        }
    }

    /**
     * 단일 FCM 토큰으로 메시지를 전송한다.
     * 전송 실패 시 [handleInvalidToken]으로 위임한다.
     */
    private fun sendToToken(fcmToken: FcmToken, userId: Long, title: String, body: String) {
        try {
            val message = Message.builder()
                .setToken(fcmToken.token)
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build(),
                )
                .build()
            firebaseMessaging.send(message)
            log.debug("FCM 알림 발송 성공: userId={}", userId)
        } catch (e: FirebaseMessagingException) {
            handleInvalidToken(fcmToken, userId, e)
        }
    }

    /**
     * 만료되거나 유효하지 않은 토큰을 DB에서 삭제한다.
     * UNREGISTERED / INVALID_ARGUMENT 에러 코드일 때만 삭제하고, 나머지는 경고만 남긴다.
     */
    private fun handleInvalidToken(fcmToken: FcmToken, userId: Long, e: FirebaseMessagingException) {
        if (e.messagingErrorCode == MessagingErrorCode.UNREGISTERED ||
            e.messagingErrorCode == MessagingErrorCode.INVALID_ARGUMENT
        ) {
            log.info("만료된 FCM 토큰 삭제: userId={}, errorCode={}", userId, e.messagingErrorCode)
            fcmTokenRepository.delete(fcmToken)
        } else {
            log.warn("FCM 알림 발송 실패: userId={}, errorCode={}", userId, e.messagingErrorCode)
        }
    }
}
