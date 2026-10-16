package com.xavierclavel.bankable.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.xavierclavel.bankable.data.db.Category
import com.xavierclavel.bankable.data.db.Subcategory
import com.xavierclavel.bankable.model.CategoryOut
import com.xavierclavel.bankable.model.SubcategoryOut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Thrown when deleting a category or subcategory that expenses still use. */
class CategoryInUseException : Exception("Category in use")

/** The categories and subcategories held locally. Every change requests a sync. */
object CategoryStore {

    /** Every category with its subcategories, in creation order. Emits again whenever they change. */
    fun observe(): Flow<List<CategoryOut>> = combine(
        database.categoryQueries.selectAll().asFlow().mapToList(Dispatchers.IO),
        database.subcategoryQueries.selectAll().asFlow().mapToList(Dispatchers.IO),
    ) { categories, subcategories ->
        val byCategory = subcategories.groupBy { it.category_id }
        categories.map { it.toOut(byCategory[it.id].orEmpty()) }
    }

    /**
     * Creates a category ([id] null), with its default subcategory, or updates one. A category's
     * type can't change once created.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun saveCategory(id: String?, name: String, type: String, color: String, icon: String) {
        writeLocal {
            if (id == null) {
                val categoryId = Uuid.random().toString()
                val defaultId = Uuid.random().toString()
                upsertCategory(categoryId, null, name, color, icon, type)
                upsertSubcategory(defaultId, null, categoryId, name, icon, type, isDefault = true)
                markPending(SyncType.CATEGORY, categoryId)
                markPending(SyncType.SUBCATEGORY, defaultId)
            } else {
                val storedType = categoryQueries.selectById(id).executeAsOneOrNull()?.type ?: type
                upsertCategory(id, null, name, color, icon, storedType)
                markPending(SyncType.CATEGORY, id)
            }
        }
        Sync.requestSync()
    }

    /**
     * Deletes a category and its subcategories.
     *
     * @throws CategoryInUseException when one of its subcategories has expenses.
     */
    suspend fun deleteCategory(id: String) {
        writeLocal {
            val subcategories = subcategoryQueries.selectByCategory(id).executeAsList()
            if (subcategories.any { subcategoryQueries.isUsed(it.id).executeAsOne() }) {
                throw CategoryInUseException()
            }
            subcategories.forEach {
                subcategoryQueries.deleteById(it.id)
                markPending(SyncType.SUBCATEGORY, it.id, deleted = true)
            }
            categoryQueries.deleteById(id)
            markPending(SyncType.CATEGORY, id, deleted = true)
        }
        Sync.requestSync()
    }

    /** Creates a subcategory ([id] null) or updates one, possibly moving it to [categoryId]. */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun saveSubcategory(id: String?, categoryId: String, name: String, icon: String, type: String) {
        writeLocal {
            val stored = id?.let { subcategoryQueries.selectById(it).executeAsOneOrNull() }
            val subcategoryId = id ?: Uuid.random().toString()
            upsertSubcategory(
                id = subcategoryId,
                serverId = null,
                categoryId = categoryId,
                name = name,
                icon = icon,
                type = stored?.type ?: type,
                isDefault = stored?.is_default == 1L,
            )
            markPending(SyncType.SUBCATEGORY, subcategoryId)
        }
        Sync.requestSync()
    }

    /**
     * Deletes a subcategory.
     *
     * @throws CategoryInUseException when it has expenses.
     */
    suspend fun deleteSubcategory(id: String) {
        writeLocal {
            if (subcategoryQueries.isUsed(id).executeAsOne()) throw CategoryInUseException()
            subcategoryQueries.deleteById(id)
            markPending(SyncType.SUBCATEGORY, id, deleted = true)
        }
        Sync.requestSync()
    }

    /** A default subcategory takes its category's name and icon; every subcategory takes its color. */
    private fun Category.toOut(subcategories: List<Subcategory>) = CategoryOut(
        id = id,
        serverId = server_id?.toInt(),
        name = name,
        color = color,
        icon = icon,
        type = type,
        subcategories = subcategories.map { sub ->
            val isDefault = sub.is_default != 0L
            SubcategoryOut(
                id = sub.id,
                serverId = sub.server_id?.toInt(),
                name = if (isDefault) "$name - Default" else sub.name,
                type = type,
                color = color,
                icon = if (isDefault) icon else sub.icon,
                isDefault = isDefault,
            )
        },
    )
}
