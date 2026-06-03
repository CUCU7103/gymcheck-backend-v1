package com.gymcheck.domain

import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import java.time.LocalDateTime

/**
 * 모든 엔티티가 공통으로 가지는 기본 식별자와 생성 시각을 제공하는 부모 클래스.
 */
@MappedSuperclass
abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set

    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    @PrePersist
    fun onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now()
        }
    }
}
