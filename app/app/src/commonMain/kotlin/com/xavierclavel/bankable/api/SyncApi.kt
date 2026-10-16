package com.xavierclavel.bankable.api

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable

/**
 * Wire format of `/sync`: rows to create or update, and rows deleted. Rows are identified by
 * uuids; pulled rows also carry their [SyncCategory.serverId], the id the REST API knows them by.
 * Amounts are plain decimal strings, dates "yyyy-MM-dd", types "EXPENSE" / "INCOME".
 */
@Serializable
data class SyncChanges(
    val categories: List<SyncCategory> = emptyList(),
    val subcategories: List<SyncSubcategory> = emptyList(),
    val tags: List<SyncTag> = emptyList(),
    val expenses: List<SyncExpense> = emptyList(),
    val deletions: List<SyncDeletion> = emptyList(),
)

@Serializable
data class SyncCategory(
    val id: String,
    val serverId: Long? = null,
    val name: String,
    val color: String,
    val icon: String,
    val type: String,
)

@Serializable
data class SyncSubcategory(
    val id: String,
    val serverId: Long? = null,
    val categoryId: String,
    val name: String,
    val icon: String,
    val type: String,
    val isDefault: Boolean,
)

@Serializable
data class SyncTag(
    val id: String,
    val serverId: Long? = null,
    val label: String,
)

@Serializable
data class SyncExpense(
    val id: String,
    val serverId: Long? = null,
    val subcategoryId: String?,
    val title: String,
    val amount: String,
    val currency: String,
    val date: String,
    val type: String,
    val tagIds: List<String> = emptyList(),
)

/** [type] is "CATEGORY", "SUBCATEGORY", "TAG" or "EXPENSE". */
@Serializable
data class SyncDeletion(
    val type: String,
    val id: String,
)

/**
 * @property revision Cursor to send as `since` on the next pull.
 * @property hasMore True when more changes are waiting: pull again right away.
 */
@Serializable
data class SyncPull(
    val revision: Long,
    val hasMore: Boolean,
    val changes: SyncChanges,
)

/** Fetches the changes made after revision [since], oldest first. */
suspend fun apiPullChanges(since: Long): SyncPull =
    httpClient.get("$BASE_URL/sync/pull") {
        authHeader()
        parameter("since", since)
    }.body()

/** Sends the changes made on this device. */
suspend fun apiPushChanges(changes: SyncChanges) {
    httpClient.post("$BASE_URL/sync/push") {
        authHeader()
        contentType(ContentType.Application.Json)
        setBody(changes)
    }
}
