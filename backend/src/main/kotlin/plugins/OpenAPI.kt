package com.xavierclavel.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.routing.*

fun Application.setupOpenAPI() {
    routing {
        openAPI(path = "openapi")
    }
}
