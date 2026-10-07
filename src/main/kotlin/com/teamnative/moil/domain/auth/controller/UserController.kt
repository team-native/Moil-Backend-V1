package com.teamnative.moil.domain.auth.controller

import com.teamnative.moil.domain.auth.dto.DeviceTokenRequest
import com.teamnative.moil.domain.auth.dto.DeviceTokenResponse
import com.teamnative.moil.domain.auth.dto.DeleteDeviceTokenRequest
import com.teamnative.moil.domain.auth.service.AuthenticatedUserService
import com.teamnative.moil.domain.auth.service.DeviceTokenService
import com.teamnative.moil.global.dto.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val authenticatedUserService: AuthenticatedUserService,
    private val deviceTokenService: DeviceTokenService,
) {

    @PutMapping("/me/device-token")
    fun saveDeviceToken(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @Valid @RequestBody request: DeviceTokenRequest,
    ): ApiResponse<DeviceTokenResponse> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)

        return ApiResponse.success(
            message = "디바이스 토큰을 저장했습니다.",
            data = deviceTokenService.save(user, request.token, request.platform),
        )
    }

    @DeleteMapping("/me/device-token")
    fun deleteDeviceToken(
        @RequestHeader("Authorization", required = false) authorization: String?,
        @Valid @RequestBody request: DeleteDeviceTokenRequest,
    ): ApiResponse<Nothing> {
        val user = authenticatedUserService.getByAuthorizationHeader(authorization)
        deviceTokenService.delete(user, request.token)

        return ApiResponse.empty("디바이스 토큰을 삭제했습니다.")
    }
}
