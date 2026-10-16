package com.xavierclavel.bankable.platform

import app.cash.sqldelight.db.SqlDriver

/** Opens the app's local SQLite database, creating its schema on first launch. */
expect fun createSqlDriver(): SqlDriver
