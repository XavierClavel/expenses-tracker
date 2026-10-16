package com.xavierclavel.bankable.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.xavierclavel.bankable.data.db.BankableDatabase

actual fun createSqlDriver(): SqlDriver =
    AndroidSqliteDriver(BankableDatabase.Schema, AppContext.appContext, "bankable.db")
