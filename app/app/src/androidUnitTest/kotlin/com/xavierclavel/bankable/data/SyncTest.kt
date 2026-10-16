package com.xavierclavel.bankable.data

import com.xavierclavel.bankable.api.SyncCategory
import com.xavierclavel.bankable.api.SyncChanges
import com.xavierclavel.bankable.api.SyncDeletion
import com.xavierclavel.bankable.api.SyncExpense
import com.xavierclavel.bankable.api.SyncPull
import com.xavierclavel.bankable.api.SyncSubcategory
import com.xavierclavel.bankable.api.SyncTag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncTest : LocalDataTest() {

    private val category = SyncCategory(id = "c1", serverId = 1, name = "Groceries", color = "red", icon = "cart", type = "EXPENSE")
    private val subcategory = SyncSubcategory(
        id = "s1", serverId = 2, categoryId = "c1", name = "Groceries", icon = "cart", type = "EXPENSE", isDefault = true,
    )
    private val tag = SyncTag(id = "t1", serverId = 3, label = "Holidays")
    private val expense = SyncExpense(
        id = "e1", serverId = 4, subcategoryId = "s1", title = "Carrefour", amount = "8.00",
        currency = "EUR", date = "2026-10-01", type = "EXPENSE", tagIds = listOf("t1"),
    )

    private suspend fun saveExpense(title: String, id: String? = null) =
        ExpenseStore.save(id, title, 1250, "EUR", "2026-10-01", null, "EXPENSE", emptySet())

    @Test
    fun `local changes are pushed with their current state, then are no longer pending`() = runBlocking {
        CategoryStore.saveCategory(id = null, name = "Groceries", type = "EXPENSE", color = "red", icon = "cart")
        val subcategoryId = CategoryStore.observe().first().single().subcategories.single().id
        TagStore.save(id = null, label = "Holidays")
        val tagId = TagStore.observe().first().single().id
        ExpenseStore.save(null, "Carrefour", 1250, "EUR", "2026-10-01", subcategoryId, "EXPENSE", setOf(tagId))

        Sync.syncNow()

        val pushed = server.pushes.single()
        assertEquals(listOf("Groceries"), pushed.categories.map { it.name })
        assertEquals(listOf(true), pushed.subcategories.map { it.isDefault })
        assertEquals(listOf("Holidays"), pushed.tags.map { it.label })
        val pushedExpense = pushed.expenses.single()
        assertEquals("12.50", pushedExpense.amount)
        assertEquals(subcategoryId, pushedExpense.subcategoryId)
        assertEquals(listOf(tagId), pushedExpense.tagIds)
        assertEquals(0L, pendingCount())

        Sync.syncNow()
        assertEquals("nothing left to push", 1, server.pushes.size)
    }

    @Test
    fun `a row deleted before it was pushed is pushed as a deletion only`() = runBlocking {
        saveExpense("Carrefour")
        val id = ExpenseStore.observe(limit = 10).first().single().id
        ExpenseStore.delete(listOf(id))

        Sync.syncNow()

        val pushed = server.pushes.single()
        assertTrue(pushed.expenses.isEmpty())
        assertEquals(listOf(SyncDeletion(SyncType.EXPENSE.name, id)), pushed.deletions)
    }

    @Test
    fun `a change made while a push is in flight is pushed by the next sync`() = runBlocking {
        saveExpense("First")
        server.duringPush = {
            server.duringPush = {}
            saveExpense("Second")
        }

        Sync.syncNow()
        assertEquals(listOf("First"), server.pushes.single().expenses.map { it.title })
        assertEquals(1L, pendingCount())

        Sync.syncNow()
        assertEquals(listOf("Second"), server.pushes.last().expenses.map { it.title })
        assertEquals(0L, pendingCount())
    }

    @Test
    fun `pulled pages are applied in turn and the cursor moves forward`() = runBlocking {
        server.pages += SyncPull(2, hasMore = true, SyncChanges(categories = listOf(category), subcategories = listOf(subcategory)))
        server.pages += SyncPull(4, hasMore = false, SyncChanges(tags = listOf(tag), expenses = listOf(expense)))

        Sync.syncNow()

        assertEquals(listOf(0L, 2L), server.pulledSince)
        val pulledCategory = CategoryStore.observe().first().single()
        assertEquals(1, pulledCategory.serverId)
        assertEquals("s1", pulledCategory.subcategories.single().id)
        val pulledExpense = ExpenseStore.observe(limit = 10).first().single()
        assertEquals(4, pulledExpense.serverId)
        assertEquals("8.00", pulledExpense.amount)
        assertEquals("s1", pulledExpense.categoryId)
        assertEquals(listOf("t1"), pulledExpense.tagIds)

        Sync.syncNow()
        assertEquals(4L, server.pulledSince.last())
    }

    @Test
    fun `pulled deletions remove the rows and their tag links`() = runBlocking {
        server.pages += SyncPull(4, hasMore = false, SyncChanges(listOf(category), listOf(subcategory), listOf(tag), listOf(expense)))
        Sync.syncNow()
        server.pages += SyncPull(5, hasMore = false, SyncChanges(deletions = listOf(SyncDeletion("TAG", "t1"))))

        Sync.syncNow()

        assertTrue(TagStore.observe().first().isEmpty())
        assertTrue(ExpenseStore.observe(limit = 10).first().single().tagIds.isEmpty())
    }

    @Test
    fun `pulled rows don't overwrite changes not pushed yet`() = runBlocking {
        server.pages += SyncPull(4, hasMore = false, SyncChanges(listOf(category), listOf(subcategory), listOf(tag), listOf(expense)))
        Sync.syncNow()
        saveExpense("Edited offline", id = "e1")

        writeLocal { with(Sync) { applyPulled(SyncChanges(expenses = listOf(expense.copy(title = "Edited elsewhere")))) } }

        assertEquals("Edited offline", ExpenseStore.observe(limit = 10).first().single().title)
    }

    @Test
    fun `clearing deletes the local data and the cursor`() = runBlocking {
        server.pages += SyncPull(4, hasMore = false, SyncChanges(listOf(category), listOf(subcategory), listOf(tag), listOf(expense)))
        Sync.syncNow()
        saveExpense("Not pushed")

        Sync.clear()

        assertTrue(ExpenseStore.observe(limit = 10).first().isEmpty())
        assertTrue(CategoryStore.observe().first().isEmpty())
        assertEquals(0L, pendingCount())
        Sync.syncNow()
        assertEquals(0L, server.pulledSince.last())
    }
}
