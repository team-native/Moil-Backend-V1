package com.teamnative.moil.domain.oauth.repository

import com.teamnative.moil.domain.oauth.helper.SocialLoginType
import com.teamnative.moil.domain.oauth.model.SocialAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SocialAccountRepository : JpaRepository<SocialAccount, Long> {
    fun findByProviderAndProviderUserId(provider: SocialLoginType, providerUserId: String): SocialAccount?

    fun findByProviderAndUserId(provider: SocialLoginType, userId: Long): SocialAccount?

    fun deleteByUserId(userId: Long)
}
