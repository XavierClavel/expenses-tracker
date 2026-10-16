package com.xavierclavel.dtos.sync

import kotlinx.serialization.Serializable

/**
 * Response of `GET /sync/pull`.
 *
 * @property revision Cursor to send as `since` on the next pull. Every change up to it has been
 * returned, by this response or an earlier one.
 * @property hasMore True when changes past [revision] already exist: pull again right away.
 * @property changes Current state of every row changed after `since`, and the rows deleted since.
 */
@Serializable
data class SyncPullOut(
    val revision: Long,
    val hasMore: Boolean,
    val changes: SyncChanges,
)
