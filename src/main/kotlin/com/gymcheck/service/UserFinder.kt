package com.gymcheck.service

import com.gymcheck.domain.user.User
import com.gymcheck.exception.CustomException
import com.gymcheck.exception.ErrorCode
import com.gymcheck.repository.UserRepository
import org.springframework.stereotype.Component

/**
 * 사용자 조회 단일 seam.
 * 모든 서비스는 UserRepository 대신 이 컴포넌트를 통해 사용자를 조회한다.
 * NOT_FOUND 에러코드로 통일되어 있으며, 조회 정책 변경 시 이 파일만 수정한다.
 */
@Component
class UserFinder(
    private val userRepository: UserRepository,
) {
    /**
     * userId로 사용자를 조회한다. 존재하지 않으면 NOT_FOUND 예외를 던진다.
     */
    fun findById(userId: Long): User =
        userRepository.findById(userId)
            .orElseThrow { CustomException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다.") }
}
