package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.BatchTagAction
import com.xavierclavel.dtos.ExpenseBatchIn
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.dtos.TagOperation
import com.xavierclavel.dtos.TagIn
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.utils.EXPENSES_URL
import com.xavierclavel.utils.TAG_URL
import com.xavierclavel.utils.assertTagDoesNotExist
import com.xavierclavel.utils.assertTagExists
import com.xavierclavel.utils.createExpense
import com.xavierclavel.utils.createTag
import com.xavierclavel.utils.deleteTag
import com.xavierclavel.utils.getExpense
import com.xavierclavel.utils.getTag
import com.xavierclavel.utils.listExpenses
import com.xavierclavel.utils.listTagsByUser
import com.xavierclavel.utils.updateExpense
import com.xavierclavel.utils.updateTag
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
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TagControllerTest: ApplicationTest() {

    val tagInTemplate = TagIn(label = "Holidays")
    val expense = ExpenseIn(
        title = "Carrefour",
        amount = BigDecimal("25.00"),
        currency = "eur",
        date = LocalDate.parse("2020-06-06"),
        categoryId = null,
        type = ExpenseType.EXPENSE,
    )

    @Test
    fun `create tag`() = runTestAsUser {
        assertEquals(0, client.listTagsByUser().size)
        val result = client.createTag(tagInTemplate)
        assertEquals("Holidays", result.label)
        assertEquals(1, client.listTagsByUser().size)
    }

    @Test
    fun `get tag`() = runTestAsUser {
        val tag = client.createTag(tagInTemplate)
        val result = client.getTag(tag.id)
        assertEquals("Holidays", result.label)
    }

    @Test
    fun `edit tag`() = runTestAsUser {
        val tag = client.createTag(tagInTemplate)
        client.updateTag(tag.id, tagInTemplate.copy(label = "Groceries"))
        assertEquals("Groceries", client.getTag(tag.id).label)
    }

    @Test
    fun `delete tag`() = runTestAsUser {
        val tag = client.createTag(tagInTemplate)
        client.assertTagExists(tag.id)
        client.deleteTag(tag.id)
        client.assertTagDoesNotExist(tag.id)
    }

    @Test
    fun `list tags is scoped per user`() = runTest {
        runAsUser1 {
            client.createTag(tagInTemplate.copy(label = "Holidays"))
        }
        runAsUser2 {
            client.createTag(tagInTemplate.copy(label = "Work"))
            client.createTag(tagInTemplate.copy(label = "Fun"))
        }

        runAsUser1 {
            val result = client.listTagsByUser()
            assertEquals(1, result.size)
            assertEquals("Holidays", result[0].label)
        }
        runAsUser2 {
            val result = client.listTagsByUser()
            assertEquals(2, result.size)
            assertTrue { result.any { it.label == "Work" } }
            assertTrue { result.any { it.label == "Fun" } }
        }
    }

    @Test
    fun `tags are ordered by label`() = runTestAsUser {
        client.createTag(tagInTemplate.copy(label = "Zebra"))
        client.createTag(tagInTemplate.copy(label = "Apple"))
        client.createTag(tagInTemplate.copy(label = "Mango"))
        val result = client.listTagsByUser()
        assertEquals(listOf("Apple", "Mango", "Zebra"), result.map { it.label })
    }

    @Test
    fun `assign multiple tags to an expense`() = runTestAsUser {
        val tag1 = client.createTag(tagInTemplate.copy(label = "Holidays"))
        val tag2 = client.createTag(tagInTemplate.copy(label = "Food"))

        val created = client.createExpense(expense.copy(tagIds = listOf(tag1.id, tag2.id)))
        assertEquals(setOf(tag1.id, tag2.id), created.tagIds.toSet())

        val fetched = client.getExpense(created.id)
        assertEquals(setOf(tag1.id, tag2.id), fetched.tagIds.toSet())
    }

    @Test
    fun `expenses can be filtered by tag`() = runTestAsUser {
        val tag1 = client.createTag(tagInTemplate.copy(label = "Holidays"))
        val tag2 = client.createTag(tagInTemplate.copy(label = "Food"))

        client.createExpense(expense.copy(title = "Hotel", tagIds = listOf(tag1.id)))
        client.createExpense(expense.copy(title = "Restaurant", tagIds = listOf(tag1.id, tag2.id)))
        client.createExpense(expense.copy(title = "Untagged"))

        val taggedWith1 = client.listExpenses(mapOf("tagId" to tag1.id.toString()))
        assertEquals(2, taggedWith1.size)
        assertTrue { taggedWith1.all { it.tagIds.contains(tag1.id) } }

        val taggedWith2 = client.listExpenses(mapOf("tagId" to tag2.id.toString()))
        assertEquals(1, taggedWith2.size)
        assertEquals("Restaurant", taggedWith2.first().title)
    }

    @Test
    fun `tag total is net income minus expenses`() = runTestAsUser {
        val tag = client.createTag(tagInTemplate.copy(label = "Holidays"))

        client.createExpense(expense.copy(amount = BigDecimal("25.00"), type = ExpenseType.EXPENSE, tagIds = listOf(tag.id)))
        client.createExpense(expense.copy(amount = BigDecimal("75.00"), type = ExpenseType.EXPENSE, tagIds = listOf(tag.id)))
        client.createExpense(expense.copy(amount = BigDecimal("30.00"), type = ExpenseType.INCOME, tagIds = listOf(tag.id)))
        client.createExpense(expense.copy(amount = BigDecimal("10.00")))

        val result = client.getTag(tag.id)
        assertEquals(3, result.expenseCount)
        // net = 30 income - (25 + 75) expenses
        assertEquals(0, BigDecimal("-70.00").compareTo(result.total))
    }

    @Test
    fun `updating expense tags replaces the links`() = runTestAsUser {
        val tag1 = client.createTag(tagInTemplate.copy(label = "Holidays"))
        val tag2 = client.createTag(tagInTemplate.copy(label = "Food"))

        val created = client.createExpense(expense.copy(amount = BigDecimal("40.00"), tagIds = listOf(tag1.id)))
        client.updateExpense(created.id, expense.copy(amount = BigDecimal("40.00"), tagIds = listOf(tag2.id)))

        assertEquals(0, client.getTag(tag1.id).expenseCount)
        assertEquals(1, client.getTag(tag2.id).expenseCount)
    }

    @Test
    fun `deleting a tag unlinks it from expenses`() = runTestAsUser {
        val tag = client.createTag(tagInTemplate)
        val created = client.createExpense(expense.copy(tagIds = listOf(tag.id)))

        client.deleteTag(tag.id)

        val fetched = client.getExpense(created.id)
        assertTrue { fetched.tagIds.isEmpty() }
    }

    @Test
    fun `cannot assign a tag owned by another user`() = runTest {
        var otherUserTagId: Long = 0
        runAsUser2 {
            otherUserTagId = client.createTag(tagInTemplate).id
        }
        runAsUser1 {
            client.post(EXPENSES_URL) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(expense.copy(tagIds = listOf(otherUserTagId)))
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, status)
            }
        }
    }

    @Test
    fun `batch assign and remove a tag over several expenses`() = runTestAsUser {
        val tag = client.createTag(tagInTemplate)
        val e1 = client.createExpense(expense.copy(title = "A"))
        val e2 = client.createExpense(expense.copy(title = "B"))

        client.put("$EXPENSES_URL/batch") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(ExpenseBatchIn(listOf(TagOperation(tag.id, BatchTagAction.ADD, listOf(e1.id, e2.id)))))
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        assertEquals(2, client.getTag(tag.id).expenseCount)
        assertTrue { client.getExpense(e1.id).tagIds.contains(tag.id) }
        assertTrue { client.getExpense(e2.id).tagIds.contains(tag.id) }

        client.put("$EXPENSES_URL/batch") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(ExpenseBatchIn(listOf(TagOperation(tag.id, BatchTagAction.REMOVE, listOf(e1.id)))))
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        assertEquals(1, client.getTag(tag.id).expenseCount)
        assertTrue { client.getExpense(e1.id).tagIds.isEmpty() }
        assertTrue { client.getExpense(e2.id).tagIds.contains(tag.id) }
    }

    @Test
    fun `batch assign is idempotent`() = runTestAsUser {
        val tag = client.createTag(tagInTemplate)
        val e1 = client.createExpense(expense.copy(tagIds = listOf(tag.id)))

        client.put("$EXPENSES_URL/batch") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(ExpenseBatchIn(listOf(TagOperation(tag.id, BatchTagAction.ADD, listOf(e1.id)))))
        }.apply { assertEquals(HttpStatusCode.OK, status) }

        assertEquals(listOf(tag.id), client.getExpense(e1.id).tagIds)
    }

    @Test
    fun `batch tag rejects a tag owned by another user`() = runTest {
        var tagId: Long = 0
        runAsUser2 { tagId = client.createTag(tagInTemplate).id }
        runAsUser1 {
            val e = client.createExpense(expense)
            client.put("$EXPENSES_URL/batch") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(ExpenseBatchIn(listOf(TagOperation(tagId, BatchTagAction.ADD, listOf(e.id)))))
            }.apply { assertEquals(HttpStatusCode.Forbidden, status) }
        }
    }

    @Test
    fun `batch tag rejects an expense owned by another user`() = runTest {
        var tagId: Long = 0
        var foreignExpenseId: Long = 0
        runAsUser1 { tagId = client.createTag(tagInTemplate).id }
        runAsUser2 { foreignExpenseId = client.createExpense(expense).id }
        runAsUser1 {
            client.put("$EXPENSES_URL/batch") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(ExpenseBatchIn(listOf(TagOperation(tagId, BatchTagAction.ADD, listOf(foreignExpenseId)))))
            }.apply { assertEquals(HttpStatusCode.Forbidden, status) }
        }
    }

    @Test
    fun `user cannot edit a tag they do not own`() = runTest {
        var tagId: Long = 0
        runAsUser1 {
            tagId = client.createTag(tagInTemplate).id
        }
        runAsUser2 {
            client.put("$TAG_URL/$tagId") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(tagInTemplate.copy(label = "Hacked"))
            }.apply {
                assertEquals(HttpStatusCode.Forbidden, status)
            }
        }
    }

    @Test
    fun `user cannot delete a tag they do not own`() = runTest {
        var tagId: Long = 0
        runAsUser1 {
            tagId = client.createTag(tagInTemplate).id
        }
        runAsUser2 {
            client.delete("$TAG_URL/$tagId").apply {
                assertEquals(HttpStatusCode.Forbidden, status)
            }
        }
    }

    @Test
    fun `users cannot see each others tags`() = runTest {
        var tagId: Long = 0
        runAsUser1 {
            tagId = client.createTag(tagInTemplate).id
        }
        runAsUser2 {
            client.get("$TAG_URL/$tagId").apply {
                assertEquals(HttpStatusCode.Forbidden, status)
            }
        }
    }
}
