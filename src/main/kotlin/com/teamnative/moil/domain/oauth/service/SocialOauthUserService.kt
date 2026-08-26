package com.teamnative.moil.domain.oauth.service

import com.teamnative.moil.domain.auth.dto.AuthTokenResponse
import com.teamnative.moil.domain.auth.model.UserAccount
import com.teamnative.moil.domain.auth.repository.UserAccountRepository
import com.teamnative.moil.domain.auth.service.AuthTokenService
import com.teamnative.moil.domain.oauth.helper.SocialLoginType
import com.teamnative.moil.domain.oauth.model.SocialAccount
import com.teamnative.moil.domain.oauth.repository.SocialAccountRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant

@Service
class SocialOauthUserService(
    private val userAccountRepository: UserAccountRepository,
    private val socialAccountRepository: SocialAccountRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authTokenService: AuthTokenService,
    private val clock: Clock,
) {

    @Transactional
    fun loginOrCreate(
        provider: SocialLoginType,
        providerUserId: String,
        email: String,
        name: String,
    ): AuthTokenResponse {
        val socialAccount = socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
        val user = socialAccount?.let { userAccountRepository.getReferenceById(it.userId) }
            ?: findOrCreateLinkedUser(provider, providerUserId, email, name)
        val token = authTokenService.issueForLogin(user)

        return AuthTokenResponse(
            userId = token.userId,
            name = token.name,
            email = token.email,
            accessToken = token.accessToken,
            refreshToken = token.refreshToken,
            tokenType = token.tokenType,
            expiresIn = token.expiresIn,
        )
    }

    private fun findOrCreateLinkedUser(
        provider: SocialLoginType,
        providerUserId: String,
        email: String,
        name: String,
    ): UserAccount {
        val user = userAccountRepository.findByEmail(email) ?: createUser(email, name)
        linkSocialAccount(provider, providerUserId, user, email)

        return user
    }

    private fun linkSocialAccount(
        provider: SocialLoginType,
        providerUserId: String,
        user: UserAccount,
        email: String,
    ) {
        val linkedAccount = socialAccountRepository.findByProviderAndUserId(provider, user.id)
        if (linkedAccount?.providerUserId == providerUserId) {
            return
        }
        if (linkedAccount != null) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "User is already linked to another $provider account.")
        }

        try {
            socialAccountRepository.saveAndFlush(
                SocialAccount(
                    userId = user.id,
                    provider = provider,
                    providerUserId = providerUserId,
                    email = email,
                    createdAt = Instant.now(clock),
                ),
            )
        } catch (exception: DataIntegrityViolationException) {
            val existingAccount = socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                ?: throw exception
            if (existingAccount.userId != user.id) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "$provider account is already linked to another user.")
            }
        }
    }

    private fun createUser(email: String, name: String): UserAccount =
        try {
            userAccountRepository.saveAndFlush(
                UserAccount(
                    name = name.takeIf { it.isNotBlank() } ?: email.substringBefore("@"),
                    email = email,
                    passwordHash = checkNotNull(passwordEncoder.encode("oauth:$email")),
                ),
            )
        } catch (exception: DataIntegrityViolationException) {
            userAccountRepository.findByEmail(email) ?: throw exception
        }
}
