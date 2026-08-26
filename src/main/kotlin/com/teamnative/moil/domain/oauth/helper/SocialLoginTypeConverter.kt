package com.teamnative.moil.domain.oauth.helper

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class SocialLoginTypeConverter : Converter<String, SocialLoginType> {
    override fun convert(source: String): SocialLoginType =
        when (source.lowercase()) {
            "google" -> SocialLoginType.GOOGLE
            "kakao" -> SocialLoginType.KAKAO
            "apple" -> SocialLoginType.APPLE
            else -> throw IllegalArgumentException("Unsupported social login type: $source")
        }
}
