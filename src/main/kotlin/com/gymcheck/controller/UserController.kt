package com.gymcheck.controller

import com.gymcheck.dto.request.UpdateGoalRequest
import com.gymcheck.dto.request.UpdateProfileRequest
import com.gymcheck.dto.response.GoalResponse
import com.gymcheck.dto.response.UserProfileResponse
import com.gymcheck.security.jwt.UserPrincipal
import com.gymcheck.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
) {

    @GetMapping("/me")
    fun getMyProfile(@AuthenticationPrincipal user: UserPrincipal): UserProfileResponse =
        userService.getProfile(user.id)

    @PutMapping("/me")
    fun updateMyProfile(
        @AuthenticationPrincipal user: UserPrincipal,
        @RequestBody @Valid request: UpdateProfileRequest,
    ): UserProfileResponse = userService.updateProfile(user.id, request)

    @GetMapping("/me/goal")
    fun getMyGoal(@AuthenticationPrincipal user: UserPrincipal): GoalResponse =
        userService.getGoal(user.id)

    @PutMapping("/me/goal")
    fun updateMyGoal(
        @AuthenticationPrincipal user: UserPrincipal,
        @RequestBody @Valid request: UpdateGoalRequest,
    ): GoalResponse = userService.updateGoal(user.id, request)

    @DeleteMapping("/me")
    fun deleteAccount(@AuthenticationPrincipal user: UserPrincipal): ResponseEntity<Unit> {
        userService.deleteAccount(user.id)
        return ResponseEntity.noContent().build()
    }
}
