package com.xavierclavel.bankable.data

import com.xavierclavel.bankable.api.SyncPull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The app reads pulls as the backend writes them. */
class SyncContractTest : LocalDataTest() {

    // GET /sync/pull?since=0, as answered by backend 1.10.0 after a push of these rows.
    private val backendPull = """
        {"revision":6,"hasMore":false,"changes":{
        "categories":[{"id":"7f1c2a8e-1111-4c2b-9a51-000000000001","serverId":1,"name":"Groceries","color":"red","icon":"cart","type":"EXPENSE"}],
        "subcategories":[{"id":"7f1c2a8e-1111-4c2b-9a51-000000000002","serverId":1,"categoryId":"7f1c2a8e-1111-4c2b-9a51-000000000001","name":"Groceries","icon":"cart","type":"EXPENSE","isDefault":true}],
        "tags":[{"id":"7f1c2a8e-1111-4c2b-9a51-000000000003","serverId":1,"label":"Trip"}],
        "expenses":[
        {"id":"7f1c2a8e-1111-4c2b-9a51-000000000004","serverId":1,"subcategoryId":"7f1c2a8e-1111-4c2b-9a51-000000000002","title":"Carrefour","amount":"12.50","currency":"EUR","date":"2026-10-01","type":"EXPENSE","tagIds":["7f1c2a8e-1111-4c2b-9a51-000000000003"]},
        {"id":"7f1c2a8e-1111-4c2b-9a51-000000000005","serverId":2,"subcategoryId":null,"title":"Gift","amount":"5.00","currency":"EUR","date":"2026-10-02","type":"INCOME","tagIds":[]}],
        "deletions":[]}}
    """.trimIndent()

    @Test
    fun `a pull written by the backend is read and applied`() = runBlocking {
        server.pages += Json { ignoreUnknownKeys = true }.decodeFromString<SyncPull>(backendPull)

        Sync.syncNow()

        val expenses = ExpenseStore.observe(limit = 10).first()
        assertEquals(listOf("Gift", "Carrefour"), expenses.map { it.title })
        assertNull(expenses.first().categoryId)
        assertEquals("12.50", expenses.last().amount)
        assertEquals(listOf("7f1c2a8e-1111-4c2b-9a51-000000000003"), expenses.last().tagIds)
        assertEquals("Groceries - Default", CategoryStore.observe().first().single().subcategories.single().name)
        assertEquals("-12.50", TagStore.observe().first().single().total)
    }
}
