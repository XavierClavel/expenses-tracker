package com.xavierclavel.bankable.data

import app.cash.sqldelight.db.SqlDriver
import com.xavierclavel.bankable.data.db.BankableDatabase
import com.xavierclavel.bankable.platform.createSqlDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The local copy of the user's categories, subcategories, tags and expenses: screens read and
 * write it, and [Sync] keeps it in step with the backend.
 */
lateinit var database: BankableDatabase
    private set

/** Opens the local database. Called from each platform's entry point, after the HTTP client. */
fun initDatabase() {
    if (!::database.isInitialized) initDatabase(createSqlDriver())
}

/** Opens the database on [driver]; tests pass an in-memory one. */
internal fun initDatabase(driver: SqlDriver) {
    database = BankableDatabase(driver)
}

/** Kinds of synced rows, as named by the sync API and the pending_change table. */
enum class SyncType { CATEGORY, SUBCATEGORY, TAG, EXPENSE }

private val writeLock = Mutex()

/**
 * Runs [block] in a transaction, one writer at a time, so that a sync applying pulled rows can't
 * interleave with a local edit and overwrite it.
 */
suspend fun <T> writeLocal(block: BankableDatabase.() -> T): T = writeLock.withLock {
    withContext(Dispatchers.IO) {
        database.transactionWithResult { database.block() }
    }
}

suspend fun <T> readLocal(block: BankableDatabase.() -> T): T =
    withContext(Dispatchers.IO) { database.block() }

/** Records that a row changed on this device, so that the next push sends it. */
internal fun BankableDatabase.markPending(type: SyncType, id: String, deleted: Boolean = false) {
    syncQueries.markPending(type.name, id, if (deleted) 1 else 0)
}

// Upserts: update the row if it exists, insert it otherwise. A null serverId keeps the known one.

internal fun BankableDatabase.upsertCategory(
    id: String,
    serverId: Long?,
    name: String,
    color: String,
    icon: String,
    type: String,
) {
    categoryQueries.update(server_id = serverId, name = name, color = color, icon = icon, type = type, id = id)
    categoryQueries.insertOrIgnore(id = id, server_id = serverId, name = name, color = color, icon = icon, type = type)
}

internal fun BankableDatabase.upsertSubcategory(
    id: String,
    serverId: Long?,
    categoryId: String,
    name: String,
    icon: String,
    type: String,
    isDefault: Boolean,
) {
    val default = if (isDefault) 1L else 0L
    subcategoryQueries.update(
        server_id = serverId,
        category_id = categoryId,
        name = name,
        icon = icon,
        type = type,
        is_default = default,
        id = id,
    )
    subcategoryQueries.insertOrIgnore(
        id = id,
        server_id = serverId,
        category_id = categoryId,
        name = name,
        icon = icon,
        type = type,
        is_default = default,
    )
}

internal fun BankableDatabase.upsertTag(id: String, serverId: Long?, label: String) {
    tagQueries.update(server_id = serverId, label = label, id = id)
    tagQueries.insertOrIgnore(id = id, server_id = serverId, label = label)
}

internal fun BankableDatabase.upsertExpense(
    id: String,
    serverId: Long?,
    subcategoryId: String?,
    title: String,
    amountCents: Long,
    currency: String,
    date: String,
    type: String,
    tagIds: Collection<String>,
) {
    expenseQueries.update(
        server_id = serverId,
        subcategory_id = subcategoryId,
        title = title,
        amount_cents = amountCents,
        currency = currency,
        date = date,
        type = type,
        id = id,
    )
    expenseQueries.insertOrIgnore(
        id = id,
        server_id = serverId,
        subcategory_id = subcategoryId,
        title = title,
        amount_cents = amountCents,
        currency = currency,
        date = date,
        type = type,
    )
    expenseQueries.removeTags(id)
    tagIds.forEach { expenseQueries.addTag(id, it) }
}

internal fun BankableDatabase.deleteExpense(id: String) {
    expenseQueries.removeTags(id)
    expenseQueries.deleteById(id)
}

internal fun BankableDatabase.deleteTag(id: String) {
    expenseQueries.removeTagEverywhere(id)
    tagQueries.deleteById(id)
}

/** Deletes everything stored locally, including changes not pushed yet. */
internal fun BankableDatabase.deleteAll() {
    expenseQueries.deleteAllTags()
    expenseQueries.deleteAll()
    subcategoryQueries.deleteAll()
    categoryQueries.deleteAll()
    tagQueries.deleteAll()
    syncQueries.deleteAllPending()
    syncQueries.deleteSyncState()
}
