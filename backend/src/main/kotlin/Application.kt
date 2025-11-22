package com.xavierclavel

import com.xavierclavel.config.koinModules
import com.xavierclavel.config.serveRoutes
import com.xavierclavel.plugins.DatabaseManager
import com.xavierclavel.plugins.configureAuthentication
import com.xavierclavel.plugins.configureMonitoring
import com.xavierclavel.plugins.configureSerialization
import com.xavierclavel.plugins.configureStatusPages
import com.xavierclavel.plugins.setupOpenAPI
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.core.context.startKoin


fun main() {
    startKoin {
        modules(
            koinModules,
        )
    }
    DatabaseManager.init()
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    setupOpenAPI()
    configureMonitoring()
    configureStatusPages()
    configureSerialization()
    configureAuthentication()
    serveRoutes()
}
