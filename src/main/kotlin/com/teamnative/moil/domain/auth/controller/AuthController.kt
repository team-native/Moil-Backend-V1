package com.teamnative.moil.domain.auth.controller

import com.teamnative.moil.domain.auth.dto.LoginRequest
import com.teamnative.moil.domain.auth.dto.LoginResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse =
        LoginResponse(
            accessToken = "replace-with-jwt-token",
            tokenType = "Bearer",
            username = request.username,
        )
}
