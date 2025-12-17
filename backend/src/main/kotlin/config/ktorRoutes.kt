package com.xavierclavel.config

import com.xavierclavel.routes.setupAccountController
import com.xavierclavel.routes.setupAccountReportController
import com.xavierclavel.routes.setupAuthController
import com.xavierclavel.routes.setupCategoryController
import com.xavierclavel.routes.setupExpenseController
import com.xavierclavel.routes.setupSubcategoryController
import com.xavierclavel.routes.setupSummaryController
import com.xavierclavel.routes.setupTrendController
import com.xavierclavel.routes.setupUserController
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing

fun Application.serveRoutes() {
    routing {
        setupUserController()
        setupAuthController()
        authenticate("bearer-auth", "auth-session") {
            setupCategoryController()
            setupSubcategoryController()
            setupExpenseController()
            setupSummaryController()
            setupTrendController()
            setupAccountController()
            setupAccountReportController()
        }
    }
}