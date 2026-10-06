package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.CategoryIn
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.dtos.IdListIn
import com.xavierclavel.dtos.RecurringExpenseIn
import com.xavierclavel.dtos.SubcategoryIn
import com.xavierclavel.dtos.TagIn
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.services.RecurringExpenseService
import com.xavierclavel.utils.CATEGORY_URL
import com.xavierclavel.utils.RECURRING_EXPENSES_URL
import com.xavierclavel.utils.SUBCATEGORY_URL
import com.xavierclavel.utils.assertRecurringExpenseDoesNotExist
import com.xavierclavel.utils.createCategory
import com.xavierclavel.utils.createExpense
import com.xavierclavel.utils.createRecurringExpense
import com.xavierclavel.utils.createRecurringExpensesFromExpenses
import com.xavierclavel.utils.createSubcategory
import com.xavierclavel.utils.createTag
import com.xavierclavel.utils.deleteRecurringExpense
import com.xavierclavel.utils.deleteTag
import com.xavierclavel.utils.getMe
import com.xavierclavel.utils.getRecurringExpense
import com.xavierclavel.utils.listExpenses
import com.xavierclavel.utils.listRecurringExpenses
import com.xavierclavel.utils.updateRecurringExpense
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.Test
import org.koin.test.inject
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecurringExpenseControllerTest: ApplicationTest() {

    val recurringExpenseService by inject<RecurringExpenseService>()

    val today: LocalDate = LocalDate.now()

    // A day of month that is never today, so the first occurrence is always in the future.
    val notToday = today.dayOfMonth % 28 + 1

    val template = RecurringExpenseIn(
        title = "Rent",
        amount = BigDecimal("800.00"),
        currency = "eur",
        categoryId = null,
        type = ExpenseType.EXPENSE,
        dayOfMonth = notToday,
    )

    val expenseTemplate = ExpenseIn(
        title = "Salary",
        categoryId = null,
        amount = BigDecimal("2500.00"),
        currency = "eur",
        date = LocalDate.of(2020, 1, notToday),
        type = ExpenseType.INCOME,
    )

    @Test
    fun `create recurring expense`() = runTestAsUser {
        val result = client.createRecurringExpense(template)
        assertEquals("Rent", result.title)
        assertEquals(notToday, result.dayOfMonth)
        assertTrue(result.nextDate.isAfter(today))
        assertEquals(notToday, result.nextDate.dayOfMonth)
        assertNull(result.lastGeneratedDate)
        assertEquals(listOf(result), client.listRecurringExpenses())
        // Its first occurrence is still ahead, so no expense exists yet.
        assertEquals(0, client.listExpenses().size)
    }

    @Test
    fun `recurring expense due today creates its expense right away`() = runTestAsUser {
        val tag = client.createTag(TagIn(label = "Home"))
        val result = client.createRecurringExpense(
            template.copy(dayOfMonth = today.dayOfMonth, type = ExpenseType.INCOME, tagIds = listOf(tag.id))
        )
        assertEquals(today, result.lastGeneratedDate)
        assertEquals(today.plusMonths(1), result.nextDate)

        val expense = client.listExpenses().single()
        assertEquals("Rent", expense.title)
        assertEquals(BigDecimal("800.00"), expense.amount)
        assertEquals("eur", expense.currency)
        assertEquals(today, expense.date)
        assertEquals(ExpenseType.INCOME, expense.type)
        assertEquals(listOf(tag.id), expense.tagIds)
    }

    @Test
    fun `generation creates one expense per month and catches up missed months`() = runTest {
        var recurringId: Long = 0
        runAsUser1 {
            val userId = client.getMe().id
            recurringId = recurringExpenseService
                .create(template.copy(dayOfMonth = 20), userId, today = LocalDate.parse("2026-01-15"))
                .id
        }

        assertEquals(0, recurringExpenseService.generateDueExpenses(LocalDate.parse("2026-01-19")))
        assertEquals(1, recurringExpenseService.generateDueExpenses(LocalDate.parse("2026-01-20")))
        // Running again on the same day must not create a duplicate.
        assertEquals(0, recurringExpenseService.generateDueExpenses(LocalDate.parse("2026-01-20")))
        // Missed runs (e.g. downtime) are caught up, each expense on its own date.
        assertEquals(3, recurringExpenseService.generateDueExpenses(LocalDate.parse("2026-04-25")))

        runAsUser1 {
            assertEquals(
                listOf("2026-01-20", "2026-02-20", "2026-03-20", "2026-04-20").map(LocalDate::parse),
                client.listExpenses().map { it.date }.sorted(),
            )
            val recurring = client.getRecurringExpense(recurringId)
            assertEquals(LocalDate.parse("2026-04-20"), recurring.lastGeneratedDate)
            assertEquals(LocalDate.parse("2026-05-20"), recurring.nextDate)
        }
    }

    @Test
    fun `day 31 falls on the last day of shorter months`() = runTest {
        runAsUser1 {
            val userId = client.getMe().id
            recurringExpenseService.create(template.copy(dayOfMonth = 31), userId, today = LocalDate.parse("2026-01-01"))
        }

        assertEquals(4, recurringExpenseService.generateDueExpenses(LocalDate.parse("2026-04-30")))

        runAsUser1 {
            assertEquals(
                listOf("2026-01-31", "2026-02-28", "2026-03-31", "2026-04-30").map(LocalDate::parse),
                client.listExpenses().map { it.date }.sorted(),
            )
        }
    }

    @Test
    fun `changing the day never creates a second expense in the same month`() = runTest {
        runAsUser1 {
            val userId = client.getMe().id
            val created = recurringExpenseService
                .create(template.copy(dayOfMonth = 5), userId, today = LocalDate.parse("2026-03-05"))
            assertEquals(LocalDate.parse("2026-03-05"), created.lastGeneratedDate)

            // March already got its expense, so moving to the 20th waits for April.
            val updated = recurringExpenseService
                .update(userId, created.id, template.copy(dayOfMonth = 20), today = LocalDate.parse("2026-03-10"))
            assertEquals(LocalDate.parse("2026-04-20"), updated.nextDate)
            assertEquals(1, client.listExpenses().size)
        }
    }

    @Test
    fun `changing the day moves the next occurrence into the current month when it had none`() = runTest {
        runAsUser1 {
            val userId = client.getMe().id
            // Created after this month's 5th: first occurrence is next month.
            val created = recurringExpenseService
                .create(template.copy(dayOfMonth = 5), userId, today = LocalDate.parse("2026-03-10"))
            assertEquals(LocalDate.parse("2026-04-05"), created.nextDate)

            val later = recurringExpenseService
                .update(userId, created.id, template.copy(dayOfMonth = 20), today = LocalDate.parse("2026-03-12"))
            assertEquals(LocalDate.parse("2026-03-20"), later.nextDate)

            // Moving to today creates this month's expense immediately.
            val dueNow = recurringExpenseService
                .update(userId, created.id, template.copy(dayOfMonth = 12), today = LocalDate.parse("2026-03-12"))
            assertEquals(LocalDate.parse("2026-03-12"), dueNow.lastGeneratedDate)
            assertEquals(LocalDate.parse("2026-04-12"), dueNow.nextDate)
            assertEquals(listOf(LocalDate.parse("2026-03-12")), client.listExpenses().map { it.date })
        }
    }

    @Test
    fun `update recurring expense leaves already created expenses untouched`() = runTestAsUser {
        val created = client.createRecurringExpense(template.copy(dayOfMonth = today.dayOfMonth))
        val updated = client.updateRecurringExpense(
            created.id,
            template.copy(dayOfMonth = today.dayOfMonth, title = "New rent", amount = BigDecimal("900.00")),
        )
        assertEquals("New rent", updated.title)
        assertEquals(BigDecimal("900.00"), updated.amount)
        assertEquals(created.nextDate, updated.nextDate)

        val expense = client.listExpenses().single()
        assertEquals("Rent", expense.title)
        assertEquals(BigDecimal("800.00"), expense.amount)
    }

    @Test
    fun `delete recurring expense keeps created expenses`() = runTestAsUser {
        val tag = client.createTag(TagIn(label = "Home"))
        val created = client.createRecurringExpense(template.copy(dayOfMonth = today.dayOfMonth, tagIds = listOf(tag.id)))
        client.deleteRecurringExpense(created.id)
        client.assertRecurringExpenseDoesNotExist(created.id)
        assertEquals(0, client.listRecurringExpenses().size)
        assertEquals(1, client.listExpenses().size)
    }

    @Test
    fun `create recurring expenses from expenses`() = runTestAsUser {
        val tag = client.createTag(TagIn(label = "Work"))
        val expense = client.createExpense(expenseTemplate.copy(tagIds = listOf(tag.id)))

        val result = client.createRecurringExpensesFromExpenses(listOf(expense.id)).single()
        assertEquals("Salary", result.title)
        assertEquals(BigDecimal("2500.00"), result.amount)
        assertEquals("eur", result.currency)
        assertEquals(ExpenseType.INCOME, result.type)
        assertEquals(listOf(tag.id), result.tagIds)
        assertEquals(notToday, result.dayOfMonth)
        assertEquals(notToday, result.nextDate.dayOfMonth)
        assertTrue(result.nextDate.isAfter(today))
        assertEquals(expense.date, result.lastGeneratedDate)
        assertEquals(listOf(result), client.listRecurringExpenses())
        // Its first occurrence is still ahead, so no expense was added.
        assertEquals(listOf(expense.id), client.listExpenses().map { it.id })
    }

    @Test
    fun `an expense made recurring counts as its month's occurrence`() = runTest {
        runAsUser1 {
            val userId = client.getMe().id
            val ids = listOf("2026-03-10", "2026-03-20", "2026-02-10", "2026-02-05").map {
                client.createExpense(expenseTemplate.copy(title = it, date = LocalDate.parse(it))).id
            }
            val created = recurringExpenseService
                .createFromExpenses(userId, ids, today = LocalDate.parse("2026-03-10"))
                .associateBy { it.title }

            // March already has these two, so neither adds another one before April.
            assertEquals(LocalDate.parse("2026-04-10"), created.getValue("2026-03-10").nextDate)
            assertEquals(LocalDate.parse("2026-04-20"), created.getValue("2026-03-20").nextDate)
            // From last month and due today: March's expense is added right away.
            assertEquals(LocalDate.parse("2026-03-10"), created.getValue("2026-02-10").lastGeneratedDate)
            assertEquals(LocalDate.parse("2026-04-10"), created.getValue("2026-02-10").nextDate)
            // From last month on a day already past: like any new recurring expense, it starts next month.
            assertEquals(LocalDate.parse("2026-04-05"), created.getValue("2026-02-05").nextDate)
            assertEquals(5, client.listExpenses().size)

            // Moving the day later in March still waits for April, since March is covered.
            val moved = recurringExpenseService.update(
                userId,
                created.getValue("2026-03-20").id,
                template.copy(dayOfMonth = 25),
                today = LocalDate.parse("2026-03-12"),
            )
            assertEquals(LocalDate.parse("2026-04-25"), moved.nextDate)
        }
    }

    @Test
    fun `cannot create recurring expenses from expenses owned by another user`() = runTest {
        var foreignId: Long = 0
        runAsUser2 { foreignId = client.createExpense(expenseTemplate).id }
        runAsUser1 {
            client.post("$RECURRING_EXPENSES_URL/from-expenses") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(IdListIn(listOf(foreignId)))
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, status)
            }
            assertEquals(0, client.listRecurringExpenses().size)
        }
    }

    @Test
    fun `invalid day of month is rejected`() = runTestAsUser {
        listOf(0, 32).forEach { day ->
            client.post(RECURRING_EXPENSES_URL) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(template.copy(dayOfMonth = day))
            }.apply {
                assertEquals(HttpStatusCode.BadRequest, status)
            }
        }
        val created = client.createRecurringExpense(template)
        client.put("$RECURRING_EXPENSES_URL/${created.id}") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(template.copy(dayOfMonth = 32))
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `deleting a tag unlinks it from recurring expenses`() = runTestAsUser {
        val tag = client.createTag(TagIn(label = "Home"))
        val created = client.createRecurringExpense(template.copy(tagIds = listOf(tag.id)))
        assertEquals(listOf(tag.id), created.tagIds)
        client.deleteTag(tag.id)
        assertEquals(emptyList(), client.getRecurringExpense(created.id).tagIds)
    }

    @Test
    fun `cannot delete a category used by a recurring expense`() = runTestAsUser {
        val category = client.createCategory(CategoryIn(name = "Housing", type = ExpenseType.EXPENSE, color = "", icon = ""))
        val subcategory = client.createSubcategory(
            SubcategoryIn(name = "Rent", type = ExpenseType.EXPENSE, icon = "", parentCategory = category.id)
        )
        client.createRecurringExpense(template.copy(categoryId = subcategory.id))
        client.delete("$SUBCATEGORY_URL/${subcategory.id}").apply {
            assertEquals(HttpStatusCode.Forbidden, status)
        }
        client.delete("$CATEGORY_URL/${category.id}").apply {
            assertEquals(HttpStatusCode.Forbidden, status)
        }
    }

    @Test
    fun `users cannot access each others recurring expenses`() = runTest {
        var recurringId: Long = 0
        runAsUser1 {
            recurringId = client.createRecurringExpense(template).id
        }
        runAsUser2 {
            assertEquals(0, client.listRecurringExpenses().size)
            client.get("$RECURRING_EXPENSES_URL/$recurringId").apply {
                assertEquals(HttpStatusCode.Forbidden, status)
            }
            client.put("$RECURRING_EXPENSES_URL/$recurringId") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(template.copy(title = "Hacked"))
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, status)
            }
            client.delete("$RECURRING_EXPENSES_URL/$recurringId").apply {
                assertEquals(HttpStatusCode.Forbidden, status)
            }
        }
        runAsUser1 {
            assertEquals("Rent", client.getRecurringExpense(recurringId).title)
        }
    }
}
