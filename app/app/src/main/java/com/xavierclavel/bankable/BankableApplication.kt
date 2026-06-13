package com.xavierclavel.bankable

import android.app.Application
import com.xavierclavel.bankable.api.initHttpClient

class BankableApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initHttpClient(this)
    }
}
