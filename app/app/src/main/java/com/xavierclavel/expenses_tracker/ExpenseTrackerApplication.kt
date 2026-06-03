package com.xavierclavel.expenses_tracker

import android.app.Application
import com.xavierclavel.expenses_tracker.api.initHttpClient

class ExpenseTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initHttpClient(this)
    }
}
