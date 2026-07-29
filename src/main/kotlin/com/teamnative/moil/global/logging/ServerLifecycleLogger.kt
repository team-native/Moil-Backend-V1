package com.teamnative.moil.global.logging

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
class ServerLifecycleLogger(
    private val appLogger: AppLogger,
    private val environment: Environment,
    @Value("\${spring.application.name:moil}")
    private val applicationName: String,
    @Value("\${server.port:8080}")
    private val serverPort: String,
) {

    @EventListener(ApplicationReadyEvent::class)
    fun logServerStarted() {
        val server = serverData()
        appLogger.info(
            AppLogDto(
                level = LogLevel.INFO,
                event = LogEvent.SERVER_STARTED,
                message = "Server started successfully.",
                server = server,
            ),
        )
    }

    @EventListener(ContextClosedEvent::class)
    fun logServerStopped() {
        val server = serverData()
        appLogger.info(
            AppLogDto(
                level = LogLevel.INFO,
                event = LogEvent.SERVER_STOPPED,
                message = "Server stopped gracefully.",
                server = server,
            ),
        )
    }

    private fun serverData(): ServerLogData =
        ServerLogData(
            application = applicationName,
            port = serverPort,
            profiles = environment.activeProfiles.toList(),
        )
}
