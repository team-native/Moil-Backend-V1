package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.model.UserAccount
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AuthTokenService {

    fun issue(user: UserAccount): IssuedToken =
        IssuedToken(
            userId = user.id,
            name = user.name,
            email = user.email,
            accessToken = "access_${UUID.randomUUID()}",
            refreshToken = "refresh_${UUID.randomUUID()}",
            tokenType = "Bearer",
            expiresIn = ACCESS_TOKEN_EXPIRES_IN_SECONDS,
        )

    data class IssuedToken(
        val userId: Long,
        val name: String,
        val email: String,
        val accessToken: String,
        val refreshToken: String,
        val tokenType: String,
        val expiresIn: Long,
    )

    companion object {
        private const val ACCESS_TOKEN_EXPIRES_IN_SECONDS = 3600L
    }
}
