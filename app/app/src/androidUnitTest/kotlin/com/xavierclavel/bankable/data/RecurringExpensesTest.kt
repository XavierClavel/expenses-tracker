package com.xavierclavel.bankable.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.xavierclavel.bankable.api.SyncChanges
import com.xavierclavel.bankable.api.SyncDeletion
import com.xavierclavel.bankable.api.SyncPull
import com.xavierclavel.bankable.api.SyncRecurringExpense
import com.xavierclavel.bankable.data.db.BankableDatabase
import com.xavierclavel.bankable.util.todayIsoDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringExpensesTest : LocalDataTest() {

    private val today = LocalDate.parse(todayIsoDate())

    // A day of month that is never today, so the first occurrence is always in the future.
    private val notToday = today.day % 28 + 1

    private suspend fun save(dayOfMonth: Int = notToday, id: String? = null, tagIds: Set<String> = emptySet()) =
        RecurringExpenseStore.save(id, "Rent", 80000, "EUR", "EXPENSE", null, tagIds, dayOfMonth)

    private suspend fun single() = RecurringExpenseStore.observe().first().single()

    @Test
    fun `a new recurring expense starts at its next occurrence`() = runBlocking {
        val dueToday = save()

        assertFalse(dueToday)
        val recurringExpense = single()
        assertEquals(nextOccurrence(notToday, today).toString(), recurringExpense.nextDate)
        assertEquals("800.00", recurringExpense.amount)
        assertNull(recurringExpense.lastGeneratedDate)
    }

    @Test
    fun `a new recurring expense on today's day is due today`() = runBlocking {
        assertTrue(save(dayOfMonth = today.day))
        assertEquals(today.toString(), single().nextDate)
    }

    @Test
    fun `changing the day of a recurring expense moves its next date`() = runBlocking {
        save()
        val newDay = notToday % 28 + 1

        save(dayOfMonth = newDay, id = single().id)

        assertEquals(newDay, LocalDate.parse(single().nextDate).day)
    }

    @Test
    fun `an expense made recurring counts as its month's occurrence`() = runBlocking {
        TagStore.save(id = null, label = "Home")
        val tag = TagStore.observe().first().single().id
        ExpenseStore.save(null, "Rent", 80000, "EUR", today.toString(), null, "EXPENSE", setOf(tag))
        val expense = ExpenseStore.observe(limit = 10).first().single()

        RecurringExpenseStore.createFromExpenses(listOf(expense.id))

        val recurringExpense = single()
        assertEquals(today.day, recurringExpense.dayOfMonth)
        assertEquals(today.toString(), recurringExpense.lastGeneratedDate)
        assertEquals(firstOccurrence(today.day, today, madeFrom = today).toString(), recurringExpense.nextDate)
        assertTrue(LocalDate.parse(recurringExpense.nextDate) > today)
        assertEquals(listOf(tag), recurringExpense.tagIds)
    }

    @Test
    fun `a subcategory used by a recurring expense can't be deleted`() = runBlocking {
        CategoryStore.saveCategory(id = null, name = "Home", type = "EXPENSE", color = "red", icon = "home")
        val category = CategoryStore.observe().first().single()
        RecurringExpenseStore.save(null, "Rent", 80000, "EUR", "EXPENSE", category.subcategories.single().id, emptySet(), notToday)

        val error = runCatching { CategoryStore.deleteCategory(category.id) }.exceptionOrNull()

        assertTrue(error is CategoryInUseException)
    }

    @Test
    fun `deleting a tag removes it from recurring expenses`() = runBlocking {
        TagStore.save(id = null, label = "Home")
        val tag = TagStore.observe().first().single().id
        save(tagIds = setOf(tag))

        TagStore.delete(tag)

        assertTrue(single().tagIds.isEmpty())
    }

    @Test
    fun `a new recurring expense is pushed with its first occurrence`() = runBlocking {
        save()
        val created = single()
        RecurringExpenseStore.delete(created.id)
        save(dayOfMonth = today.day)

        Sync.syncNow()

        val pushed = server.pushes.single()
        val recurringExpense = pushed.recurringExpenses.single()
        assertEquals(today.toString(), recurringExpense.nextDate)
        assertEquals("800.00", recurringExpense.amount)
        assertEquals(listOf(SyncDeletion(SyncType.RECURRING_EXPENSE.name, created.id)), pushed.deletions)
    }

    @Test
    fun `pulled recurring expenses are applied, then deleted`() = runBlocking {
        val pulled = SyncRecurringExpense(
            id = "r1", serverId = 7, subcategoryId = null, title = "Gym", amount = "30.00", currency = "EUR",
            type = "EXPENSE", tagIds = emptyList(), dayOfMonth = 3, nextDate = "2026-11-03", lastGeneratedDate = "2026-10-03",
        )
        server.pages += SyncPull(2, hasMore = false, SyncChanges(recurringExpenses = listOf(pulled)))

        Sync.syncNow()

        val recurringExpense = single()
        assertEquals("Gym", recurringExpense.title)
        assertEquals("2026-11-03", recurringExpense.nextDate)
        assertEquals("2026-10-03", recurringExpense.lastGeneratedDate)

        server.pages += SyncPull(3, hasMore = false, SyncChanges(deletions = listOf(SyncDeletion("RECURRING_EXPENSE", "r1"))))
        Sync.syncNow()
        assertTrue(RecurringExpenseStore.observe().first().isEmpty())
    }

    @Test
    fun `the database of a previous version gets the recurring expense tables`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        BankableDatabase.Schema.create(driver)
        // Back to version 1, which had no recurring expenses.
        driver.execute(null, "DROP TABLE recurring_expense_tag", 0)
        driver.execute(null, "DROP TABLE recurring_expense", 0)

        BankableDatabase.Schema.migrate(driver, oldVersion = 1, newVersion = BankableDatabase.Schema.version)

        val database = BankableDatabase(driver)
        database.recurringExpenseQueries.insertOrIgnore("r1", null, null, "Gym", 3000, "EUR", "EXPENSE", 3, "2026-11-03", null)
        assertEquals(1, database.recurringExpenseQueries.selectWithTags().executeAsList().size)
    }
}
