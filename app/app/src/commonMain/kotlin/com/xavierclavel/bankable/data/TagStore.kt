package com.xavierclavel.bankable.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.xavierclavel.bankable.model.TagOut
import com.xavierclavel.bankable.util.formatCents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** The tags held locally. Every change requests a sync. */
object TagStore {

    /** Every tag with its totals, sorted by label. Emits again whenever they change. */
    fun observe(): Flow<List<TagOut>> =
        database.tagQueries.selectWithTotals()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows ->
                rows.map {
                    TagOut(
                        id = it.id,
                        label = it.label,
                        total = formatCents(it.total_cents),
                        expenseCount = it.expense_count.toInt(),
                    )
                }
            }

    /** Creates a tag ([id] null) or renames one. */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun save(id: String?, label: String) {
        writeLocal {
            val tagId = id ?: Uuid.random().toString()
            upsertTag(tagId, null, label)
            markPending(SyncType.TAG, tagId)
        }
        Sync.requestSync()
    }

    /** Deletes a tag, removing it from its expenses. */
    suspend fun delete(id: String) {
        writeLocal {
            deleteTag(id)
            markPending(SyncType.TAG, id, deleted = true)
        }
        Sync.requestSync()
    }
}
