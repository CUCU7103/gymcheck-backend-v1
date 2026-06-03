package com.gymcheck.domain.workout

import com.gymcheck.domain.BaseEntity
import com.gymcheck.domain.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * 운동 기록에 사용할 운동 종류.
 *
 * 기본 운동은 전체 사용자에게 노출되고, user가 있으면 특정 사용자가 만든 커스텀 항목이다.
 */
@Entity
@Table(name = "exercise_types")
class ExerciseType(
    @Column(nullable = false, length = 100)
    var name: String,

    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    var user: User? = null,
) : BaseEntity()
