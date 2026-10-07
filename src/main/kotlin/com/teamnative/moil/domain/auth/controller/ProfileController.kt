package com.teamnative.moil.domain.auth.controller

import com.teamnative.moil.domain.auth.dto.ProfileResponse
import com.teamnative.moil.domain.auth.service.AuthenticatedUserService
import com.teamnative.moil.domain.auth.service.ProfileService
import com.teamnative.moil.global.dto.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth/profile")
class ProfileController(
    private val authenticatedUserService: AuthenticatedUserService,
    private val profileService: ProfileService,
) {

    @GetMapping
    fun getProfile(
        @RequestHeader("Authorization", required = false) authorization: String?,
    ): ApiResponse<ProfileResponse> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)

        return ApiResponse.success(
            message = "프로필을 조회했습니다.",
            data = profileService.get(user),
        )
    }
}
