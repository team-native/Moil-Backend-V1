package com.teamnative.moil.domain.auth.service

import com.teamnative.moil.domain.auth.dto.DeviceTokenResponse
import com.teamnative.moil.domain.auth.model.DevicePlatform
import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.model.UserDeviceToken
import com.teamnative.moil.domain.auth.repository.UserDeviceTokenRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant

@Service
class DeviceTokenService(
    private val userDeviceTokenRepository: UserDeviceTokenRepository,
    private val clock: Clock,
) {

    @Transactional
    fun save(user: UserAccount, rawToken: String?, rawPlatform: String?): DeviceTokenResponse {
        val token = rawToken?.trim().orEmpty()
        if (token.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "디바이스 토큰을 입력해주세요.")
        }
        val platform = parsePlatform(rawPlatform)
        val now = Instant.now(clock)
        val current = userDeviceTokenRepository.findByToken(token)
        val saved = userDeviceTokenRepository.save(
            current?.copy(userId = user.id, platform = platform, updatedAt = now)
                ?: UserDeviceToken(
                    userId = user.id,
                    token = token,
                    platform = platform,
                    createdAt = now,
                    updatedAt = now,
                ),
        )

        return DeviceTokenResponse(saved.token, saved.platform)
    }

    @Transactional
    fun delete(user: UserAccount, rawToken: String?) {
        val token = rawToken?.trim().orEmpty()
        if (token.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "디바이스 토큰을 입력해주세요.")
        }
        userDeviceTokenRepository.deleteByUserIdAndToken(user.id, token)
    }

    private fun parsePlatform(rawPlatform: String?): DevicePlatform =
        runCatching { DevicePlatform.valueOf(rawPlatform?.trim()?.uppercase().orEmpty()) }
            .getOrElse { throw ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 디바이스 플랫폼입니다.") }
}
