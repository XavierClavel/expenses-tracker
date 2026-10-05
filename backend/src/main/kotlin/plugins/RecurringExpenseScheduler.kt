package com.xavierclavel.plugins

import com.xavierclavel.services.RecurringExpenseService
import com.xavierclavel.utils.logger
import io.ktor.server.application.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.ktor.ext.inject
import kotlin.time.Duration.Companion.hours

/**
 * Create the expenses of due recurring expenses at startup, then every hour. Generation is
 * idempotent, so the hourly cadence only bounds how late after midnight an expense appears,
 * and a restart or downtime catches up on missed occurrences.
 */
fun Application.scheduleRecurringExpenses() {
    val recurringExpenseService by inject<RecurringExpenseService>()
    launch(Dispatchers.IO) {
        while (isActive) {
            try {
                val created = recurringExpenseService.generateDueExpenses()
                if (created > 0) {
                    logger.info { "Created $created expense(s) from recurring expenses" }
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to create expenses from recurring expenses" }
            }
            delay(1.hours)
        }
    }
}
