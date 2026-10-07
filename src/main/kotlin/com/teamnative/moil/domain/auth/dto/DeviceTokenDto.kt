package com.teamnative.moil.domain.auth.dto

import com.teamnative.moil.domain.auth.model.DevicePlatform
import jakarta.validation.constraints.NotBlank

data class DeviceTokenRequest(
    @field:NotBlank(message = "디바이스 토큰을 입력해주세요.")
    val token: String?,

    @field:NotBlank(message = "디바이스 플랫폼을 입력해주세요.")
    val platform: String?,
)

data class DeleteDeviceTokenRequest(
    @field:NotBlank(message = "디바이스 토큰을 입력해주세요.")
    val token: String?,
)

data class DeviceTokenResponse(
    val token: String,
    val platform: DevicePlatform,
)
