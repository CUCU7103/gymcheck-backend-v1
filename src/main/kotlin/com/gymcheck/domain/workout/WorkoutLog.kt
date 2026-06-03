package com.gymcheck.domain.workout

import com.gymcheck.domain.BaseEntity
import com.gymcheck.domain.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate

/**
 * 특정 날짜에 기록된 사용자의 운동 로그.
 *
 * 사용자와 운동 종류를 연결하고, 메모를 함께 저장한다.
 */
@Entity
@Table(name = "workout_logs")
class WorkoutLog(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exercise_type_id", nullable = false)
    var exerciseType: ExerciseType,

    @Column(name = "log_date", nullable = false)
    var logDate: LocalDate,

    @Column(length = 500)
    var memo: String? = null,
) : BaseEntity()
