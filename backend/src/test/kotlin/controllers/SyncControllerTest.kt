package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.BatchTagAction
import com.xavierclavel.dtos.CategoryIn
import com.xavierclavel.dtos.ExpenseBatchIn
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.dtos.TagIn
import com.xavierclavel.dtos.TagOperation
import com.xavierclavel.dtos.sync.SyncCategory
import com.xavierclavel.dtos.sync.SyncChanges
import com.xavierclavel.dtos.sync.SyncDeletion
import com.xavierclavel.dtos.sync.SyncExpense
import com.xavierclavel.dtos.sync.SyncSubcategory
import com.xavierclavel.dtos.sync.SyncTag
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.enums.SyncEntityType
import com.xavierclavel.utils.EXPENSES_URL
import com.xavierclavel.utils.SYNC_URL
import com.xavierclavel.utils.createCategory
import com.xavierclavel.utils.createExpense
import com.xavierclavel.utils.createTag
import com.xavierclavel.utils.deleteExpense
import com.xavierclavel.utils.deleteTag
import com.xavierclavel.utils.deleteUser
import com.xavierclavel.utils.getExpense
import com.xavierclavel.utils.listCategoriesByUser
import com.xavierclavel.utils.listExpenses
import com.xavierclavel.utils.pull
import com.xavierclavel.utils.push
import com.xavierclavel.utils.updateExpense
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyncControllerTest: ApplicationTest() {

    val expenseIn = ExpenseIn(
        title = "Carrefour",
        amount = BigDecimal("25.00"),
        currency = "eur",
        date = LocalDate.parse("2020-06-06"),
        categoryId = null,
        type = ExpenseType.EXPENSE,
    )

    fun category(name: String = "Groceries") = SyncCategory(
        id = UUID.randomUUID(),
        name = name,
        color = "#ff0000",
        icon = "cart",
        type = ExpenseType.EXPENSE,
    )

    fun subcategory(category: SyncCategory, isDefault: Boolean = false) = SyncSubcategory(
        id = UUID.randomUUID(),
        categoryId = category.id,
        name = if (isDefault) category.name else "Supermarket",
        icon = category.icon,
        type = category.type,
        isDefault = isDefault,
    )

    fun tag(label: String = "Holidays") = SyncTag(id = UUID.randomUUID(), label = label)

    fun expense(subcategoryId: UUID? = null, tagIds: List<UUID> = emptyList()) = SyncExpense(
        id = UUID.randomUUID(),
        subcategoryId = subcategoryId,
        title = "Carrefour",
        amount = BigDecimal("25.00"),
        currency = "eur",
        date = LocalDate.parse("2020-06-06"),
        type = ExpenseType.EXPENSE,
        tagIds = tagIds,
    )

    /** The changes as pushed: the server adds each row's REST id when pulled. */
    fun SyncChanges.withoutServerIds() = copy(
        categories = categories.map { it.copy(serverId = null) },
        subcategories = subcategories.map { it.copy(serverId = null) },
        tags = tags.map { it.copy(serverId = null) },
        expenses = expenses.map { it.copy(serverId = null) },
    )

    @Test
    fun `first pull returns everything created through the REST API`() = runTestAsUser {
        val category = client.createCategory(CategoryIn(name = "Groceries", type = ExpenseType.EXPENSE, color = "", icon = ""))
        val tag = client.createTag(TagIn(label = "Holidays"))
        val expense = client.createExpense(expenseIn.copy(categoryId = category.subcategories.single().id, tagIds = listOf(tag.id)))

        val result = client.pull()
        assertFalse(result.hasMore)
        val pulledCategory = result.changes.categories.single()
        val pulledSubcategory = result.changes.subcategories.single()
        val pulledTag = result.changes.tags.single()
        val pulledExpense = result.changes.expenses.single()
        assertEquals("Groceries", pulledCategory.name)
        assertEquals(pulledCategory.id, pulledSubcategory.categoryId)
        assertTrue(pulledSubcategory.isDefault)
        assertEquals("Holidays", pulledTag.label)
        assertEquals("Carrefour", pulledExpense.title)
        assertEquals(BigDecimal("25.00"), pulledExpense.amount)
        assertEquals(pulledSubcategory.id, pulledExpense.subcategoryId)
        assertEquals(listOf(pulledTag.id), pulledExpense.tagIds)
        assertEquals(category.id, pulledCategory.serverId)
        assertEquals(category.subcategories.single().id, pulledSubcategory.serverId)
        assertEquals(tag.id, pulledTag.serverId)
        assertEquals(expense.id, pulledExpense.serverId)

        val next = client.pull(since = result.revision)
        assertEquals(SyncChanges(), next.changes)
        assertEquals(result.revision, next.revision)
    }

    @Test
    fun `pull returns only the rows changed since the cursor`() = runTestAsUser {
        val first = client.createExpense(expenseIn.copy(title = "First"))
        client.createExpense(expenseIn.copy(title = "Second"))
        val cursor = client.pull().revision

        client.updateExpense(first.id, expenseIn.copy(title = "First, edited"))

        val result = client.pull(since = cursor)
        assertEquals(listOf("First, edited"), result.changes.expenses.map { it.title })
        assertTrue(result.revision > cursor)
    }

    @Test
    fun `a row deleted through the REST API is pulled as a deletion`() = runTestAsUser {
        val created = client.createExpense(expenseIn)
        val initial = client.pull()
        val uuid = initial.changes.expenses.single().id

        client.deleteExpense(created.id)

        val result = client.pull(since = initial.revision)
        assertEquals(listOf(SyncDeletion(SyncEntityType.EXPENSE, uuid)), result.changes.deletions)
        assertTrue(result.changes.expenses.isEmpty())
    }

    @Test
    fun `tagging and untagging an expense is pulled as a change of the expense`() = runTestAsUser {
        val created = client.createExpense(expenseIn)
        val tag = client.createTag(TagIn(label = "Holidays"))
        val initial = client.pull()
        val tagUuid = initial.changes.tags.single().id

        client.put("$EXPENSES_URL/batch") {
            contentType(ContentType.Application.Json)
            setBody(ExpenseBatchIn(listOf(TagOperation(tag.id, BatchTagAction.ADD, listOf(created.id)))))
        }
        val tagged = client.pull(since = initial.revision)
        assertEquals(listOf(listOf(tagUuid)), tagged.changes.expenses.map { it.tagIds })

        client.deleteTag(tag.id)
        val untagged = client.pull(since = tagged.revision)
        assertEquals(listOf(SyncDeletion(SyncEntityType.TAG, tagUuid)), untagged.changes.deletions)
        assertEquals(listOf(emptyList<UUID>()), untagged.changes.expenses.map { it.tagIds })
    }

    @Test
    fun `pull pages through the changes in revision order`() = runTestAsUser {
        repeat(5) { client.createTag(TagIn(label = "Tag $it")) }

        val labels = mutableListOf<String>()
        var cursor = 0L
        var pages = 0
        do {
            val page = client.pull(since = cursor, limit = 2)
            assertTrue(page.revision > cursor)
            labels += page.changes.tags.map { it.label }
            cursor = page.revision
            pages++
        } while (page.hasMore)

        assertEquals(3, pages)
        assertEquals((0 until 5).map { "Tag $it" }, labels)
    }

    @Test
    fun `push creates rows with the ids the device chose`() = runTestAsUser {
        val category = category()
        val default = subcategory(category, isDefault = true)
        val supermarket = subcategory(category)
        val tag = tag()
        val expense = expense(subcategoryId = supermarket.id, tagIds = listOf(tag.id))
        client.push(SyncChanges(listOf(category), listOf(default, supermarket), listOf(tag), listOf(expense)))

        val restCategory = client.listCategoriesByUser().single()
        assertEquals(2, restCategory.subcategories.size)
        val restExpense = client.listExpenses().single()
        assertEquals(restCategory.subcategories.single { !it.isDefault }.id, restExpense.categoryId)
        assertEquals(1, restExpense.tagIds.size)

        val pulled = client.pull().changes.withoutServerIds()
        assertEquals(listOf(category), pulled.categories)
        assertEquals(setOf(default, supermarket), pulled.subcategories.toSet())
        assertEquals(listOf(tag), pulled.tags)
        assertEquals(listOf(expense), pulled.expenses)
    }

    @Test
    fun `push creates the default subcategory of a new category when the device sent none`() = runTestAsUser {
        val category = category()
        client.push(SyncChanges(categories = listOf(category)))

        val default = client.pull().changes.subcategories.single()
        assertEquals(category.id, default.categoryId)
        assertTrue(default.isDefault)
    }

    @Test
    fun `pushing the same changes twice changes nothing`() = runTestAsUser {
        val category = category()
        val default = subcategory(category, isDefault = true)
        val tag = tag()
        val changes = SyncChanges(
            categories = listOf(category),
            subcategories = listOf(default),
            tags = listOf(tag),
            expenses = listOf(expense(subcategoryId = default.id, tagIds = listOf(tag.id))),
        )
        client.push(changes)
        val cursor = client.pull().revision

        client.push(changes)

        val result = client.pull(since = cursor)
        assertEquals(SyncChanges(), result.changes)
        assertEquals(1, client.listExpenses().size)
    }

    @Test
    fun `push updates rows created through the REST API`() = runTestAsUser {
        val created = client.createExpense(expenseIn)
        val pulled = client.pull().changes.expenses.single()

        client.push(SyncChanges(expenses = listOf(pulled.copy(title = "Edited on a device"))))

        assertEquals("Edited on a device", client.getExpense(created.id).title)
    }

    @Test
    fun `a deleted row is not brought back by a device that edited it`() = runTestAsUser {
        val expense = expense()
        client.push(SyncChanges(expenses = listOf(expense)))
        client.push(SyncChanges(deletions = listOf(SyncDeletion(SyncEntityType.EXPENSE, expense.id))))
        // A device that already pulled the deletion, but still had an edit of it to push.
        val cursor = client.pull().revision

        client.push(SyncChanges(expenses = listOf(expense.copy(title = "Edited offline"))))

        assertTrue(client.listExpenses().isEmpty())
        val result = client.pull(since = cursor)
        assertEquals(listOf(SyncDeletion(SyncEntityType.EXPENSE, expense.id)), result.changes.deletions)
    }

    @Test
    fun `a category whose subcategory is used by an expense is not deleted`() = runTestAsUser {
        val category = category()
        val default = subcategory(category, isDefault = true)
        client.push(SyncChanges(categories = listOf(category), subcategories = listOf(default)))
        // Another device adds an expense in the category...
        client.push(SyncChanges(expenses = listOf(expense(subcategoryId = default.id))))
        val cursor = client.pull().revision

        // ...while this one deletes the category, which it saw unused.
        client.push(
            SyncChanges(
                deletions = listOf(
                    SyncDeletion(SyncEntityType.SUBCATEGORY, default.id),
                    SyncDeletion(SyncEntityType.CATEGORY, category.id),
                )
            )
        )

        assertEquals(1, client.listCategoriesByUser().size)
        val result = client.pull(since = cursor).changes.withoutServerIds()
        assertEquals(listOf(category), result.categories)
        assertEquals(listOf(default), result.subcategories)
        assertTrue(result.deletions.isEmpty())
    }

    @Test
    fun `an expense whose subcategory is gone becomes uncategorized`() = runTestAsUser {
        val category = category()
        val default = subcategory(category, isDefault = true)
        client.push(SyncChanges(categories = listOf(category), subcategories = listOf(default)))
        client.push(SyncChanges(deletions = listOf(SyncDeletion(SyncEntityType.CATEGORY, category.id))))
        val cursor = client.pull().revision

        val expense = expense(subcategoryId = default.id, tagIds = listOf(UUID.randomUUID()))
        client.push(SyncChanges(expenses = listOf(expense)))

        assertNull(client.listExpenses().single().categoryId)
        val pulled = client.pull(since = cursor).changes.withoutServerIds().expenses.single()
        assertEquals(expense.copy(subcategoryId = null, tagIds = emptyList()), pulled)
    }

    @Test
    fun `a new subcategory whose category is gone is refused`() = runTestAsUser {
        val subcategory = subcategory(category())

        client.push(SyncChanges(subcategories = listOf(subcategory)))

        val result = client.pull()
        assertTrue(result.changes.subcategories.isEmpty())
        assertEquals(listOf(SyncDeletion(SyncEntityType.SUBCATEGORY, subcategory.id)), result.changes.deletions)
    }

    @Test
    fun `an edit of an immutable field comes back with the server's version`() = runTestAsUser {
        val expense = expense()
        client.push(SyncChanges(expenses = listOf(expense)))
        val cursor = client.pull().revision

        client.push(SyncChanges(expenses = listOf(expense.copy(type = ExpenseType.INCOME))))

        assertEquals(listOf(expense), client.pull(since = cursor).changes.withoutServerIds().expenses)
    }

    @Test
    fun `rows of another user can't be pushed nor pulled`() = runTest {
        val expense = expense()
        runAsUser1 {
            client.push(SyncChanges(expenses = listOf(expense)))
        }
        runAsUser2 {
            assertEquals(SyncChanges(), client.pull().changes)
            client.push(SyncChanges(expenses = listOf(expense.copy(title = "Hijacked"))), HttpStatusCode.Forbidden)
            client.push(
                SyncChanges(deletions = listOf(SyncDeletion(SyncEntityType.EXPENSE, expense.id))),
                HttpStatusCode.Forbidden,
            )
        }
        runAsUser1 {
            assertEquals(listOf(expense), client.pull().changes.withoutServerIds().expenses)
        }
    }

    @Test
    fun `a user with synced deletions can delete their account`() = runTestAsUser {
        val expense = expense()
        client.push(SyncChanges(expenses = listOf(expense)))
        client.push(SyncChanges(deletions = listOf(SyncDeletion(SyncEntityType.EXPENSE, expense.id))))
        client.createCategory(CategoryIn(name = "Groceries", type = ExpenseType.EXPENSE, color = "", icon = ""))

        client.deleteUser()
    }

    @Test
    fun `pull rejects a cursor that is not a number`() = runTestAsUser {
        assertEquals(HttpStatusCode.BadRequest, client.get("$SYNC_URL/pull?since=abc").status)
    }
}
