package com.xavierclavel.bankable.data

import com.xavierclavel.bankable.api.ApiException
import com.xavierclavel.bankable.api.SyncCategory
import com.xavierclavel.bankable.api.SyncChanges
import com.xavierclavel.bankable.api.SyncDeletion
import com.xavierclavel.bankable.api.SyncExpense
import com.xavierclavel.bankable.api.SyncRecurringExpense
import com.xavierclavel.bankable.api.SyncSubcategory
import com.xavierclavel.bankable.api.SyncTag
import com.xavierclavel.bankable.api.apiPullChanges
import com.xavierclavel.bankable.api.apiPushChanges
import com.xavierclavel.bankable.api.sessionToken
import com.xavierclavel.bankable.data.db.BankableDatabase
import com.xavierclavel.bankable.data.db.Category
import com.xavierclavel.bankable.data.db.Expense
import com.xavierclavel.bankable.data.db.Recurring_expense
import com.xavierclavel.bankable.data.db.Subcategory
import com.xavierclavel.bankable.data.db.Tag
import com.xavierclavel.bankable.platform.scheduleBackgroundSync
import com.xavierclavel.bankable.storage.TokenStorage
import com.xavierclavel.bankable.util.formatCents
import com.xavierclavel.bankable.util.parseCents
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the local database in step with the backend: pushes the changes made on this device,
 * then pulls the ones made elsewhere (another device, a recurring expense coming due). The
 * server settles conflicts; whatever it keeps comes back with the pull.
 */
object Sync {
    private const val DEBOUNCE_MS = 500L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val syncLock = Mutex()
    private var scheduled: Job? = null

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    /** Off in tests, which sync explicitly. */
    internal var autoSync = true

    /**
     * Syncs shortly, in the background. Calls in quick succession sync once. When it fails with
     * changes still pending (e.g. offline), the system's background scheduler delivers them.
     */
    fun requestSync() {
        if (!autoSync) return
        scheduled?.cancel()
        scheduled = scope.launch {
            delay(DEBOUNCE_MS)
            // Launched apart from [scheduled], so that a new request can't cancel a sync midway.
            scope.launch {
                try {
                    syncNow()
                } catch (_: Exception) {
                    handOffPendingChanges()
                }
            }
        }
    }

    /**
     * The app left the foreground: the system's background scheduler delivers the changes still
     * pending, even if the app gets closed before it syncs.
     */
    fun onAppBackgrounded() {
        if (!autoSync) return
        scope.launch { handOffPendingChanges() }
    }

    suspend fun hasPendingChanges(): Boolean = readLocal { syncQueries.countPending().executeAsOne() > 0 }

    private suspend fun handOffPendingChanges() {
        if (hasPendingChanges()) scheduleBackgroundSync()
    }

    /**
     * Syncs on behalf of the system's background scheduler, which may have started the app for
     * it: the session token is loaded with [loadToken] if the UI hasn't done it.
     *
     * @return false when the sync should be retried later (no connection, server error), true
     * when it's done, or when retrying can't help (logged out, rejected request).
     */
    suspend fun runInBackground(loadToken: () -> String? = { TokenStorage().loadToken() }): Boolean {
        if (sessionToken == null) sessionToken = loadToken() ?: return true
        return try {
            syncNow()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            e.status !in 500..599
        } catch (_: Exception) {
            false
        }
    }

    /** Pushes this device's changes, then pulls everyone else's. Throws when the server can't be reached. */
    suspend fun syncNow() = syncLock.withLock {
        _isSyncing.value = true
        try {
            push()
            pull()
        } finally {
            _isSyncing.value = false
        }
    }

    /** Deletes the local data, once any sync in flight is over. Run on logout. */
    suspend fun clear() {
        scheduled?.cancel()
        syncLock.withLock { writeLocal { deleteAll() } }
    }

    private suspend fun push() {
        val (changes, lastSeq) = writeLocal { pendingChanges() } ?: return
        apiPushChanges(changes)
        // Changes made while the push was in flight got a higher seq: they stay pending.
        writeLocal { syncQueries.clearPendingUpTo(lastSeq) }
    }

    private suspend fun pull() {
        var since = readLocal { syncQueries.selectRevision().executeAsOneOrNull() } ?: 0L
        do {
            val page = apiPullChanges(since)
            writeLocal {
                applyPulled(page.changes)
                syncQueries.setRevision(page.revision)
            }
            since = page.revision
        } while (page.hasMore)
    }

    /** The current state of every row changed on this device, and the seq of the last change. */
    private fun BankableDatabase.pendingChanges(): Pair<SyncChanges, Long>? {
        val pending = syncQueries.selectPending().executeAsList()
        if (pending.isEmpty()) return null
        fun changed(type: SyncType) = pending.filter { it.type == type.name && it.deleted == 0L }.map { it.id }
        val changes = SyncChanges(
            categories = changed(SyncType.CATEGORY).mapNotNull {
                categoryQueries.selectById(it).executeAsOneOrNull()?.toSync()
            },
            subcategories = changed(SyncType.SUBCATEGORY).mapNotNull {
                subcategoryQueries.selectById(it).executeAsOneOrNull()?.toSync()
            },
            tags = changed(SyncType.TAG).mapNotNull {
                tagQueries.selectById(it).executeAsOneOrNull()?.toSync()
            },
            expenses = changed(SyncType.EXPENSE).mapNotNull { id ->
                expenseQueries.selectById(id).executeAsOneOrNull()
                    ?.toSync(expenseQueries.selectTagIds(id).executeAsList())
            },
            recurringExpenses = changed(SyncType.RECURRING_EXPENSE).mapNotNull { id ->
                recurringExpenseQueries.selectById(id).executeAsOneOrNull()
                    ?.toSync(recurringExpenseQueries.selectTagIds(id).executeAsList())
            },
            deletions = pending.filter { it.deleted != 0L }.map { SyncDeletion(type = it.type, id = it.id) },
        )
        return changes to pending.maxOf { it.seq }
    }

    /**
     * Applies pulled changes. Rows changed on this device since the last push are skipped: the
     * next push sends them, and the pull after it brings back whatever the server kept.
     */
    internal fun BankableDatabase.applyPulled(changes: SyncChanges) {
        val pending = syncQueries.selectPending().executeAsList().map { it.type to it.id }.toSet()
        fun isPending(type: SyncType, id: String) = (type.name to id) in pending

        changes.categories.filterNot { isPending(SyncType.CATEGORY, it.id) }.forEach {
            upsertCategory(it.id, it.serverId, it.name, it.color, it.icon, it.type)
        }
        changes.subcategories.filterNot { isPending(SyncType.SUBCATEGORY, it.id) }.forEach {
            upsertSubcategory(it.id, it.serverId, it.categoryId, it.name, it.icon, it.type, it.isDefault)
        }
        changes.tags.filterNot { isPending(SyncType.TAG, it.id) }.forEach {
            upsertTag(it.id, it.serverId, it.label)
        }
        changes.expenses.filterNot { isPending(SyncType.EXPENSE, it.id) }.forEach {
            upsertExpense(
                id = it.id,
                serverId = it.serverId,
                subcategoryId = it.subcategoryId,
                title = it.title,
                amountCents = parseCents(it.amount) ?: 0,
                currency = it.currency,
                date = it.date,
                type = it.type,
                tagIds = it.tagIds,
            )
        }
        changes.recurringExpenses.filterNot { isPending(SyncType.RECURRING_EXPENSE, it.id) }.forEach {
            upsertRecurringExpense(
                id = it.id,
                serverId = it.serverId,
                subcategoryId = it.subcategoryId,
                title = it.title,
                amountCents = parseCents(it.amount) ?: 0,
                currency = it.currency,
                type = it.type,
                dayOfMonth = it.dayOfMonth,
                // The server always sends it.
                nextDate = it.nextDate.orEmpty(),
                lastGeneratedDate = it.lastGeneratedDate,
                tagIds = it.tagIds,
            )
        }
        changes.deletions.forEach { deletion ->
            val type = SyncType.entries.find { it.name == deletion.type } ?: return@forEach
            if (isPending(type, deletion.id)) return@forEach
            when (type) {
                SyncType.CATEGORY -> categoryQueries.deleteById(deletion.id)
                SyncType.SUBCATEGORY -> subcategoryQueries.deleteById(deletion.id)
                SyncType.TAG -> deleteTag(deletion.id)
                SyncType.EXPENSE -> deleteExpense(deletion.id)
                SyncType.RECURRING_EXPENSE -> deleteRecurringExpense(deletion.id)
            }
        }
    }

    private fun Category.toSync() = SyncCategory(
        id = id,
        name = name,
        color = color,
        icon = icon,
        type = type,
    )

    private fun Subcategory.toSync() = SyncSubcategory(
        id = id,
        categoryId = category_id,
        name = name,
        icon = icon,
        type = type,
        isDefault = is_default != 0L,
    )

    private fun Tag.toSync() = SyncTag(
        id = id,
        label = label,
    )

    private fun Recurring_expense.toSync(tagIds: List<String>) = SyncRecurringExpense(
        id = id,
        subcategoryId = subcategory_id,
        title = title,
        amount = formatCents(amount_cents),
        currency = currency,
        type = type,
        tagIds = tagIds,
        dayOfMonth = day_of_month.toInt(),
        nextDate = next_date,
        lastGeneratedDate = last_generated_date,
    )

    private fun Expense.toSync(tagIds: List<String>) = SyncExpense(
        id = id,
        subcategoryId = subcategory_id,
        title = title,
        amount = formatCents(amount_cents),
        currency = currency,
        date = date,
        type = type,
        tagIds = tagIds,
    )
}
