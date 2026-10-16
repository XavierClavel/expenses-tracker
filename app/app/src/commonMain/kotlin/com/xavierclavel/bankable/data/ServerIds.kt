package com.xavierclavel.bankable.data

import com.xavierclavel.bankable.data.db.BankableDatabase
import kotlin.coroutines.cancellation.CancellationException

/**
 * Thrown when an action goes through the REST API, which knows rows by their server id, for a
 * row the server doesn't know yet (created on this device while offline).
 */
class NotSyncedException : Exception("Not synced yet")

/**
 * The REST ids of the [type] rows in [ids], in the same order. Syncs first when some of them
 * don't have one yet.
 *
 * @throws NotSyncedException when the server still doesn't know one of them, e.g. offline.
 */
suspend fun requireServerIds(type: SyncType, ids: Collection<String>): List<Int> {
    var known = readLocal { serverIdsOf(type, ids) }
    if (ids.any { known[it] == null }) {
        try {
            Sync.syncNow()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Offline: reported below for the rows still missing.
        }
        known = readLocal { serverIdsOf(type, ids) }
    }
    return ids.map { known[it] ?: throw NotSyncedException() }
}

/** The uuids of the tags the REST API knows by [serverIds]. Tags not held locally are left out. */
suspend fun localTagIds(serverIds: Collection<Int>): Set<String> = readLocal {
    tagQueries.selectIdsByServerIds(serverIds.map { it.toLong() }).executeAsList().toSet()
}

private fun BankableDatabase.serverIdsOf(type: SyncType, ids: Collection<String>): Map<String, Int?> =
    when (type) {
        SyncType.CATEGORY -> categoryQueries.selectServerIds(ids).executeAsList().associate { it.id to it.server_id }
        SyncType.SUBCATEGORY -> subcategoryQueries.selectServerIds(ids).executeAsList().associate { it.id to it.server_id }
        SyncType.TAG -> tagQueries.selectServerIds(ids).executeAsList().associate { it.id to it.server_id }
        SyncType.EXPENSE -> expenseQueries.selectServerIds(ids).executeAsList().associate { it.id to it.server_id }
    }.mapValues { it.value?.toInt() }
