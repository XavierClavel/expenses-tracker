package com.xavierclavel.controllers

import com.xavierclavel.ApplicationTest
import com.xavierclavel.dtos.ApiKeyIn
import com.xavierclavel.dtos.ExpenseIn
import com.xavierclavel.dtos.ExpenseOut
import com.xavierclavel.dtos.TagIn
import com.xavierclavel.dtos.TagLookupOut
import com.xavierclavel.enums.ExpenseType
import com.xavierclavel.exceptions.ForbiddenCause
import com.xavierclavel.services.API_KEY_PREFIX
import com.xavierclavel.utils.API_KEY_URL
import com.xavierclavel.utils.AUTH_URL
import com.xavierclavel.utils.CATEGORY_URL
import com.xavierclavel.utils.EXPENSES_URL
import com.xavierclavel.utils.SUBCATEGORY_URL
import com.xavierclavel.utils.TAG_URL
import com.xavierclavel.utils.assertExpenseExists
import com.xavierclavel.utils.createApiKey
import com.xavierclavel.utils.createExpense
import com.xavierclavel.utils.createTag
import com.xavierclavel.utils.deleteApiKey
import com.xavierclavel.utils.deleteUser
import com.xavierclavel.utils.getExpense
import com.xavierclavel.utils.listApiKeys
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiKeyControllerTest: ApplicationTest() {

    val expense = ExpenseIn(
        title = "Carrefour",
        amount = BigDecimal("25.00"),
        currency = "eur",
        date = LocalDate.parse("2020-06-06"),
        categoryId = null,
        type = ExpenseType.EXPENSE,
    )

    private suspend fun HttpClient.createExpenseWithKey(key: String): HttpResponse =
        this.post(EXPENSES_URL) {
            bearerAuth(key)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(expense)
        }

    private suspend fun assertApiKeyNotAllowed(response: HttpResponse) {
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(ForbiddenCause.API_KEY_NOT_ALLOWED.key, response.bodyAsText())
    }

    @Test
    fun `create api key`() = runTestAsUser {
        val created = client.createApiKey("Tasker")
        assertTrue(created.key.startsWith(API_KEY_PREFIX))
        assertEquals("Tasker", created.name)
        assertEquals("$API_KEY_PREFIX…${created.key.takeLast(4)}", created.hint)
        assertNull(created.lastUsedAt)
        assertEquals(listOf(created.id), client.listApiKeys().map { it.id })
    }

    @Test
    fun `listing api keys never returns the key itself`() = runTestAsUser {
        val created = client.createApiKey("Tasker")
        client.get(API_KEY_URL).apply {
            assertEquals(HttpStatusCode.OK, status)
            assertFalse(bodyAsText().contains(created.key))
        }
    }

    @Test
    fun `blank api key name is rejected`() = runTestAsUser {
        client.post(API_KEY_URL) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(ApiKeyIn(name = "  "))
        }.apply {
            assertEquals(HttpStatusCode.BadRequest, status)
        }
    }

    @Test
    fun `api key creates expenses for its owner`() = runTest {
        var key = ""
        runAsUser1 { key = client.createApiKey("Tasker").key }

        // Logged out: the key alone authenticates the request.
        val created = client.createExpenseWithKey(key).run {
            assertEquals(HttpStatusCode.OK, status)
            Json.decodeFromString<ExpenseOut>(bodyAsText())
        }

        runAsUser1 {
            assertEquals(created, client.getExpense(created.id))
            assertNotNull(client.listApiKeys().single().lastUsedAt)
        }
    }

    @Test
    fun `api key looks up categories, subcategories and tags without totals`() = runTest {
        var key = ""
        var tagId = 0L
        runAsUser1 {
            key = client.createApiKey("Tasker").key
            tagId = client.createTag(TagIn(label = "Holidays")).id
            client.createExpense(expense.copy(tagIds = listOf(tagId)))
        }

        client.get(CATEGORY_URL) { bearerAuth(key) }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        client.get(SUBCATEGORY_URL) { bearerAuth(key) }.apply {
            assertEquals(HttpStatusCode.OK, status)
        }
        client.get(TAG_URL) { bearerAuth(key) }.apply {
            assertEquals(HttpStatusCode.OK, status)
            // Strict decoding: fails if the per-tag totals leak through.
            assertEquals(listOf(TagLookupOut(id = tagId, label = "Holidays")), Json.decodeFromString<List<TagLookupOut>>(bodyAsText()))
        }
    }

    @Test
    fun `api key is rejected outside its scope`() = runTest {
        var key = ""
        var expenseId = 0L
        runAsUser1 {
            key = client.createApiKey("Tasker").key
            expenseId = client.createExpense(expense).id
        }

        assertApiKeyNotAllowed(client.get(EXPENSES_URL) { bearerAuth(key) })
        assertApiKeyNotAllowed(client.get("$EXPENSES_URL/$expenseId") { bearerAuth(key) })
        assertApiKeyNotAllowed(client.delete("$EXPENSES_URL/$expenseId") { bearerAuth(key) })
        assertApiKeyNotAllowed(client.get(API_KEY_URL) { bearerAuth(key) })
        assertApiKeyNotAllowed(client.post(API_KEY_URL) {
            bearerAuth(key)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(ApiKeyIn(name = "Escalation"))
        })
        // Routes outside the main authenticated block don't know API keys at all.
        client.get("$AUTH_URL/me") { bearerAuth(key) }.apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }

        runAsUser1 {
            client.assertExpenseExists(expenseId)
            assertEquals(1, client.listApiKeys().size)
        }
    }

    @Test
    fun `api key narrows a request that also carries a session`() = runTestAsUser {
        val key = client.createApiKey("Tasker").key
        assertApiKeyNotAllowed(client.get(EXPENSES_URL) { bearerAuth(key) })
    }

    @Test
    fun `revoked api key is rejected`() = runTest {
        var key = ""
        runAsUser1 {
            val created = client.createApiKey("Tasker")
            key = created.key
            client.deleteApiKey(created.id)
            assertEquals(0, client.listApiKeys().size)
        }
        assertEquals(HttpStatusCode.Unauthorized, client.createExpenseWithKey(key).status)
    }

    @Test
    fun `unknown api key is rejected`() = runTest {
        assertEquals(HttpStatusCode.Unauthorized, client.createExpenseWithKey("${API_KEY_PREFIX}unknown").status)
    }

    @Test
    fun `cannot revoke another user's api key`() = runTest {
        var id = 0L
        runAsUser1 { id = client.createApiKey("Tasker").id }
        runAsUser2 {
            client.delete("$API_KEY_URL/$id").apply {
                assertEquals(HttpStatusCode.Forbidden, status)
            }
            assertEquals(0, client.listApiKeys().size)
        }
        runAsUser1 { assertEquals(1, client.listApiKeys().size) }
    }

    @Test
    fun `deleting the user revokes their api keys`() = runTest {
        var key = ""
        runAsUser1 {
            key = client.createApiKey("Tasker").key
            client.deleteUser()
        }
        assertEquals(HttpStatusCode.Unauthorized, client.createExpenseWithKey(key).status)
    }
}
