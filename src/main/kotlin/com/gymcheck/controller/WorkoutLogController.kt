package com.gymcheck.controller

import com.gymcheck.dto.request.CreateWorkoutLogRequest
import com.gymcheck.dto.response.WorkoutLogResponse
import com.gymcheck.security.jwt.UserPrincipal
import com.gymcheck.service.WorkoutLogService
import jakarta.validation.Valid
import java.time.LocalDate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/workout-logs")
class WorkoutLogController(
    private val workoutLogService: WorkoutLogService,
) {

    @GetMapping
    fun getWorkoutLogs(
        @AuthenticationPrincipal user: UserPrincipal,
        @RequestParam logDate: LocalDate,
    ): List<WorkoutLogResponse> = workoutLogService.getWorkoutLogs(user.id, logDate)

    @PostMapping
    fun createWorkoutLog(
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody request: CreateWorkoutLogRequest,
    ): ResponseEntity<WorkoutLogResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(workoutLogService.createWorkoutLog(user.id, request))
    }

    @DeleteMapping("/{workoutLogId}")
    fun deleteWorkoutLog(
        @AuthenticationPrincipal user: UserPrincipal,
        @PathVariable workoutLogId: Long,
    ): ResponseEntity<Void> {
        workoutLogService.deleteWorkoutLog(user.id, workoutLogId)
        return ResponseEntity.noContent().build()
    }
}
