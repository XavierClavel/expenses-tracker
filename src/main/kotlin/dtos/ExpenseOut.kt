package dtos

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/**
 * Represents a category in the system.
 *
 * @property name Display name of the category.
 */
@Serializable
data class ExpenseOut(
    val id: Long,
    val label: String,
    val categoryId: Long?,
    val amount: Double,
    val currency: String,

    @Contextual
    val date: LocalDateTime,
)