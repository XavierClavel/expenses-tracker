package com.xavierclavel.dtos

import kotlinx.serialization.Serializable

/**
 * Request body for a batch update over expenses. Each field is an independent list of
 * operations; add sibling lists (e.g. categoryOperations) here to grow batch editing
 * without introducing new endpoints.
 *
 * @property tagOperations Tag add/remove operations, each scoped to its own expenses.
 */
@Serializable
data class ExpenseBatchIn(
    val tagOperations: List<TagOperation> = emptyList(),
)

/**
 * A single tag mutation applied to a set of expenses.
 *
 * @property tagId The tag to add or remove.
 * @property operation Whether to add or remove the tag.
 * @property expenseIds The expenses this operation applies to.
 */
@Serializable
data class TagOperation(
    val tagId: Long,
    val operation: BatchTagAction,
    val expenseIds: List<Long>,
)

enum class BatchTagAction { ADD, REMOVE }
