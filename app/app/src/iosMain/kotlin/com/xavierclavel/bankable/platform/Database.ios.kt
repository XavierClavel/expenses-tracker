package com.xavierclavel.bankable.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.xavierclavel.bankable.data.db.BankableDatabase

actual fun createSqlDriver(): SqlDriver =
    NativeSqliteDriver(BankableDatabase.Schema, "bankable.db")
