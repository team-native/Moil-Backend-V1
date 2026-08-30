package com.teamnative.moil.domain.auth.dto

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequest(
    @field:NotBlank(message = "리프레시 토큰을 입력해주세요.")
    @JsonAlias("refreshToken")
    @JsonProperty("refresh_token")
    val refreshToken: String,
)
