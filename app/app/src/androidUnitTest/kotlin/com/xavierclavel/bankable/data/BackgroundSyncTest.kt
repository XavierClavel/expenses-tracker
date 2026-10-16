package com.xavierclavel.bankable.data

import com.xavierclavel.bankable.api.sessionToken
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The sync the system's background scheduler runs: what it reports decides whether it is retried. */
class BackgroundSyncTest : LocalDataTest() {

    private suspend fun saveExpense() =
        ExpenseStore.save(null, "Carrefour", 1250, "EUR", "2026-10-01", null, "EXPENSE", emptySet())

    @Test
    fun `pending changes are known`() = runBlocking {
        assertFalse(Sync.hasPendingChanges())
        saveExpense()
        assertTrue(Sync.hasPendingChanges())
    }

    @Test
    fun `a background sync delivers the pending changes`() = runBlocking {
        saveExpense()

        assertTrue(Sync.runInBackground())

        assertEquals(1, server.pushes.size)
        assertFalse(Sync.hasPendingChanges())
    }

    @Test
    fun `a background sync without a connection is retried`() = runBlocking {
        saveExpense()
        server.unreachable = true

        assertFalse(Sync.runInBackground())
        assertTrue(Sync.hasPendingChanges())
    }

    @Test
    fun `a background sync the server fails is retried`() = runBlocking {
        saveExpense()
        server.failWith = HttpStatusCode.ServiceUnavailable

        assertFalse(Sync.runInBackground())
        assertTrue(Sync.hasPendingChanges())
    }

    @Test
    fun `a background sync whose session was rejected is not retried`() = runBlocking {
        saveExpense()
        server.failWith = HttpStatusCode.Unauthorized

        assertTrue(Sync.runInBackground())
    }

    @Test
    fun `a background sync when logged out does nothing`() = runBlocking {
        sessionToken = null

        assertTrue(Sync.runInBackground(loadToken = { null }))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a background sync loads the session the UI didn't`() = runBlocking {
        sessionToken = null

        assertTrue(Sync.runInBackground(loadToken = { "stored token" }))
        assertEquals("stored token", sessionToken)
    }
}
