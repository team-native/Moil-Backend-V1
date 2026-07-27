package com.teamnative.moil.domain.auth.repository

import com.teamnative.moil.domain.auth.model.UserAccount
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Repository
class UserAccountRepository {
    private val sequence = AtomicLong(0)
    private val users = ConcurrentHashMap<String, UserAccount>()

    fun existsByEmail(email: String): Boolean = users.containsKey(email)

    fun save(name: String, email: String, password: String): UserAccount {
        val user = UserAccount(
            id = sequence.incrementAndGet(),
            name = name,
            email = email,
            password = password,
        )
        users[email] = user

        return user
    }
}
