package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.RecurringExpenseIn
import com.xavierclavel.dtos.TagIn
import com.xavierclavel.dtos.sync.SyncCategory
import com.xavierclavel.dtos.sync.SyncChanges
import com.xavierclavel.dtos.sync.SyncDeletion
import com.xavierclavel.dtos.sync.SyncExpense
import com.xavierclavel.dtos.sync.SyncRecurringExpense
import com.xavierclavel.dtos.sync.SyncSubcategory
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.enums.SyncEntityType
import com.xavierclavel.services.occurrenceIn
import com.xavierclavel.utils.createRecurringExpense
import com.xavierclavel.utils.createTag
import com.xavierclavel.utils.listCategoriesByUser
import com.xavierclavel.utils.listExpenses
import com.xavierclavel.utils.listRecurringExpenses
import com.xavierclavel.utils.pull
import com.xavierclavel.utils.push
import io.ktor.http.HttpStatusCode
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyncRecurringExpenseTest: ApplicationTest() {

    val today: LocalDate = LocalDate.now()

    // A day of month that is never today, so the first occurrence is always in the future.
    val notToday = today.dayOfMonth % 28 + 1

    fun recurringExpense(
        dayOfMonth: Int = notToday,
        subcategoryId: UUID? = null,
        lastGeneratedDate: LocalDate? = null,
    ) = SyncRecurringExpense(
        id = UUID.randomUUID(),
        subcategoryId = subcategoryId,
        title = "Rent",
        amount = BigDecimal("800.00"),
        currency = "eur",
        type = ExpenseType.EXPENSE,
        dayOfMonth = dayOfMonth,
        lastGeneratedDate = lastGeneratedDate,
    )

    @Test
    fun `a recurring expense created through the REST API is pulled`() = runTestAsUser {
        val tag = client.createTag(TagIn(label = "Home"))
        val created = client.createRecurringExpense(
            RecurringExpenseIn(
                title = "Rent",
                amount = BigDecimal("800.00"),
                currency = "eur",
                categoryId = null,
                type = ExpenseType.EXPENSE,
                tagIds = listOf(tag.id),
                dayOfMonth = notToday,
            )
        )

        val pulled = client.pull().changes
        val recurringExpense = pulled.recurringExpenses.single()
        assertEquals(created.id, recurringExpense.serverId)
        assertEquals(notToday, recurringExpense.dayOfMonth)
        assertEquals(created.nextDate, recurringExpense.nextDate)
        assertEquals(listOf(pulled.tags.single().id), recurringExpense.tagIds)
    }

    @Test
    fun `a pushed recurring expense due today creates its expense`() = runTestAsUser {
        val pushed = recurringExpense(dayOfMonth = today.dayOfMonth)

        client.push(SyncChanges(recurringExpenses = listOf(pushed)))

        assertEquals(listOf(today), client.listExpenses().map { it.date })
        val pulled = client.pull().changes
        assertEquals(listOf(today), pulled.expenses.map { it.date })
        val recurringExpense = pulled.recurringExpenses.single()
        assertEquals(pushed.id, recurringExpense.id)
        assertEquals(today, recurringExpense.lastGeneratedDate)
        assertEquals(occurrenceIn(YearMonth.from(today).plusMonths(1), today.dayOfMonth), recurringExpense.nextDate)
    }

    @Test
    fun `a recurring expense made from an expense doesn't repeat it that month`() = runTestAsUser {
        val expense = SyncExpense(
            id = UUID.randomUUID(),
            subcategoryId = null,
            title = "Rent",
            amount = BigDecimal("800.00"),
            currency = "eur",
            date = today,
            type = ExpenseType.EXPENSE,
        )

        client.push(
            SyncChanges(
                expenses = listOf(expense),
                recurringExpenses = listOf(recurringExpense(dayOfMonth = today.dayOfMonth, lastGeneratedDate = today)),
            )
        )

        assertEquals(1, client.listExpenses().size)
        val recurringExpense = client.listRecurringExpenses().single()
        assertEquals(today, recurringExpense.lastGeneratedDate)
        assertEquals(YearMonth.from(today).plusMonths(1), YearMonth.from(recurringExpense.nextDate))
    }

    @Test
    fun `a recurring expense created offline starts on the date the device set`() = runTestAsUser {
        val yesterday = today.minusDays(1)

        client.push(SyncChanges(recurringExpenses = listOf(recurringExpense(dayOfMonth = yesterday.dayOfMonth).copy(nextDate = yesterday))))

        assertEquals(listOf(yesterday), client.listExpenses().map { it.date })
        assertEquals(yesterday, client.listRecurringExpenses().single().lastGeneratedDate)
    }

    @Test
    fun `changing the day of a recurring expense moves its next date`() = runTestAsUser {
        val pushed = recurringExpense()
        client.push(SyncChanges(recurringExpenses = listOf(pushed)))
        val cursor = client.pull().revision
        val newDay = notToday % 28 + 1

        client.push(SyncChanges(recurringExpenses = listOf(pushed.copy(dayOfMonth = newDay))))

        val pulled = client.pull(since = cursor).changes.recurringExpenses.single()
        assertEquals(newDay, pulled.dayOfMonth)
        assertEquals(newDay, pulled.nextDate!!.dayOfMonth)
    }

    @Test
    fun `pushing the same recurring expense twice changes nothing`() = runTestAsUser {
        val pushed = recurringExpense()
        client.push(SyncChanges(recurringExpenses = listOf(pushed)))
        val cursor = client.pull().revision

        client.push(SyncChanges(recurringExpenses = listOf(pushed)))

        assertEquals(SyncChanges(), client.pull(since = cursor).changes)
    }

    @Test
    fun `a deleted recurring expense keeps the expenses it created`() = runTestAsUser {
        val pushed = recurringExpense(dayOfMonth = today.dayOfMonth)
        client.push(SyncChanges(recurringExpenses = listOf(pushed)))
        val cursor = client.pull().revision

        client.push(SyncChanges(deletions = listOf(SyncDeletion(SyncEntityType.RECURRING_EXPENSE, pushed.id))))

        assertTrue(client.listRecurringExpenses().isEmpty())
        assertEquals(1, client.listExpenses().size)
        val pulled = client.pull(since = cursor).changes
        assertEquals(listOf(SyncDeletion(SyncEntityType.RECURRING_EXPENSE, pushed.id)), pulled.deletions)
    }

    @Test
    fun `a subcategory used by a recurring expense is not deleted`() = runTestAsUser {
        val category = SyncCategory(UUID.randomUUID(), name = "Home", color = "red", icon = "home", type = ExpenseType.EXPENSE)
        val default = SyncSubcategory(
            UUID.randomUUID(), categoryId = category.id, name = "Home", icon = "home", type = ExpenseType.EXPENSE, isDefault = true,
        )
        client.push(
            SyncChanges(
                categories = listOf(category),
                subcategories = listOf(default),
                recurringExpenses = listOf(recurringExpense(subcategoryId = default.id)),
            )
        )

        client.push(SyncChanges(deletions = listOf(SyncDeletion(SyncEntityType.CATEGORY, category.id))))

        assertEquals(1, client.listCategoriesByUser().size)
    }

    @Test
    fun `a recurring expense on an impossible day is refused`() = runTestAsUser {
        val pushed = recurringExpense(dayOfMonth = 32)

        client.push(SyncChanges(recurringExpenses = listOf(pushed)))

        assertTrue(client.listRecurringExpenses().isEmpty())
        assertEquals(listOf(SyncDeletion(SyncEntityType.RECURRING_EXPENSE, pushed.id)), client.pull().changes.deletions)
    }

    @Test
    fun `recurring expenses of another user can't be pushed`() = runTest {
        val pushed = recurringExpense()
        runAsUser1 {
            client.push(SyncChanges(recurringExpenses = listOf(pushed)))
        }
        runAsUser2 {
            client.push(SyncChanges(recurringExpenses = listOf(pushed.copy(title = "Hijacked"))), HttpStatusCode.Forbidden)
        }
        runAsUser1 {
            assertEquals("Rent", client.listRecurringExpenses().single().title)
        }
    }
}
