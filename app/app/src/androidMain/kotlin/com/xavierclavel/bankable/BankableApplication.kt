package com.xavierclavel.bankable

import android.app.Application
import com.xavierclavel.bankable.api.initHttpClient
import com.xavierclavel.bankable.data.initDatabase
import com.xavierclavel.bankable.platform.AppContext

class BankableApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Must come first: the platform stores, the HTTP client and the database all
        // resolve their storage through AppContext.
        AppContext.init(this)
        initHttpClient()
        initDatabase()
    }
}
