package com.gymcheck.controller

import com.gymcheck.dto.request.CreateExerciseTypeRequest
import com.gymcheck.dto.request.UpdateExerciseTypeRequest
import com.gymcheck.dto.response.ExerciseTypeResponse
import com.gymcheck.security.jwt.UserPrincipal
import com.gymcheck.service.ExerciseTypeService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/exercise-types")
class ExerciseTypeController(
    private val exerciseTypeService: ExerciseTypeService,
) {

    @GetMapping
    fun getExerciseTypes(
        @AuthenticationPrincipal user: UserPrincipal,
    ): List<ExerciseTypeResponse> = exerciseTypeService.getExerciseTypes(user.id)

    @PostMapping
    fun createExerciseType(
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody request: CreateExerciseTypeRequest,
    ): ResponseEntity<ExerciseTypeResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(exerciseTypeService.createCustomExerciseType(user.id, request))
    }

    @PutMapping("/{exerciseTypeId}")
    fun updateExerciseType(
        @AuthenticationPrincipal user: UserPrincipal,
        @PathVariable exerciseTypeId: Long,
        @Valid @RequestBody request: UpdateExerciseTypeRequest,
    ): ExerciseTypeResponse = exerciseTypeService.updateCustomExerciseType(user.id, exerciseTypeId, request)

    @DeleteMapping("/{exerciseTypeId}")
    fun deleteExerciseType(
        @AuthenticationPrincipal user: UserPrincipal,
        @PathVariable exerciseTypeId: Long,
    ): ResponseEntity<Void> {
        exerciseTypeService.deleteCustomExerciseType(user.id, exerciseTypeId)
        return ResponseEntity.noContent().build()
    }
}
