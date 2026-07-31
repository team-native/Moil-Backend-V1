package com.teamnative.moil.global.config

import org.flywaydb.core.api.callback.Callback
import org.flywaydb.core.api.callback.Context
import org.flywaydb.core.api.callback.Event
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FlywayMigrationLoggingCallback : Callback {
    private val logger = LoggerFactory.getLogger(FlywayMigrationLoggingCallback::class.java)

    override fun supports(event: Event, context: Context): Boolean =
        event in LOGGED_EVENTS

    override fun canHandleInTransaction(event: Event, context: Context): Boolean = true

    override fun handle(event: Event, context: Context) {
        val migration = context.migrationInfo
        val migrationLabel = migration
            ?.let { "${it.version ?: "repeatable"} - ${it.description}" }
            ?: "none"

        when (event) {
            Event.BEFORE_MIGRATE -> logger.info("Flyway migration started.")
            Event.AFTER_MIGRATE -> logger.info("Flyway migration finished.")
            Event.BEFORE_EACH_MIGRATE -> logger.info("Flyway applying migration: {}", migrationLabel)
            Event.AFTER_EACH_MIGRATE -> logger.info("Flyway applied migration: {}", migrationLabel)
            else -> Unit
        }
    }

    override fun getCallbackName(): String = "flywayMigrationLoggingCallback"

    companion object {
        private val LOGGED_EVENTS = setOf(
            Event.BEFORE_MIGRATE,
            Event.AFTER_MIGRATE,
            Event.BEFORE_EACH_MIGRATE,
            Event.AFTER_EACH_MIGRATE,
        )
    }
}
