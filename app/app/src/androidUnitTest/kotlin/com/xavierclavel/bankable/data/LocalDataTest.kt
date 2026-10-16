package com.xavierclavel.bankable.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.xavierclavel.bankable.api.httpClient
import com.xavierclavel.bankable.data.db.BankableDatabase
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.junit.Before

/** Runs each test on an empty in-memory database, with [server] standing in for the backend. */
abstract class LocalDataTest {
    protected val server = FakeSyncServer()

    @Before
    fun setUpLocalData() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BankableDatabase.Schema.create(driver)
        initDatabase(driver)
        Sync.autoSync = false
        httpClient = HttpClient(MockEngine { request -> with(server) { handle(request) } }) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    protected suspend fun pendingCount(): Long = readLocal { syncQueries.countPending().executeAsOne() }
}
