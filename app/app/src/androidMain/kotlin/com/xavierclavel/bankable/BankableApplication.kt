package com.xavierclavel.bankable

import android.app.Application
import com.xavierclavel.bankable.api.initHttpClient
import com.xavierclavel.bankable.platform.AppContext

class BankableApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Must come first: the platform stores and the HTTP client both resolve
        // their storage through AppContext.
        AppContext.init(this)
        initHttpClient()
    }
}
