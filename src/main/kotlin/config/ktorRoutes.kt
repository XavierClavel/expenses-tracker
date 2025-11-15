package com.xavierclavel.config

import com.xavierclavel.routes.setupAuthController
import com.xavierclavel.routes.setupUserController
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.serveRoutes() {
    routing {
        setupUserController()
        setupAuthController()
    }
}