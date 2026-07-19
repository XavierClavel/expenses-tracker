package com.xavierclavel.bankable.tags

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.bankable.api.apiBatchDeleteExpenses
import com.xavierclavel.bankable.api.apiBatchTagExpenses
import com.xavierclavel.bankable.api.apiCreateTag
import com.xavierclavel.bankable.api.apiDeleteTag
import com.xavierclavel.bankable.api.apiListExpenses
import com.xavierclavel.bankable.api.apiListTags
import com.xavierclavel.bankable.api.apiUpdateTag
import com.xavierclavel.bankable.model.ExpenseOut
import com.xavierclavel.bankable.model.TagIn
import com.xavierclavel.bankable.model.TagOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TagsViewModel : ViewModel() {

    private val _tags = MutableStateFlow<List<TagOut>>(emptyList())
    val tags: StateFlow<List<TagOut>> = _tags

    // Expenses linked to the tag currently being viewed (paginated).
    private val _tagExpenses = MutableStateFlow<List<ExpenseOut>>(emptyList())
    val tagExpenses: StateFlow<List<ExpenseOut>> = _tagExpenses

    var selectedTag by mutableStateOf<TagOut?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isLoadingExpenses by mutableStateOf(false)
        private set

    private var tagExpensePage = 0
    private var tagExpenseHasMore = true
    private val tagExpensePageSize = 50

    init {
        loadTags()
    }

    private suspend fun fetchTags() {
        _tags.value = apiListTags()
    }

    fun loadTags() {
        viewModelScope.launch {
            isLoading = true
            try {
                fetchTags()
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    fun prepareNewTag() {
        selectedTag = null
    }

    fun prepareEditTag(tag: TagOut) {
        selectedTag = tag
    }

    /** Opens the tag detail view and (re)loads its linked expenses from the first page. */
    fun prepareViewTag(tag: TagOut) {
        selectedTag = tag
        _tagExpenses.value = emptyList()
        tagExpensePage = 0
        tagExpenseHasMore = true
        loadTagExpenses()
    }

    private fun loadTagExpenses() {
        val tagId = selectedTag?.id ?: return
        if (isLoadingExpenses || !tagExpenseHasMore) return
        viewModelScope.launch {
            isLoadingExpenses = true
            try {
                val newExpenses = apiListExpenses(tagExpensePage, tagExpensePageSize, tagId = tagId)
                _tagExpenses.value =
                    if (tagExpensePage == 0) newExpenses else _tagExpenses.value + newExpenses
                tagExpenseHasMore = newExpenses.size == tagExpensePageSize
                tagExpensePage++
            } catch (_: Exception) {
            } finally {
                isLoadingExpenses = false
            }
        }
    }

    fun loadMoreTagExpenses() {
        if (!isLoadingExpenses && tagExpenseHasMore) loadTagExpenses()
    }

    /** Adds or removes [tagId] on the selected expenses of the viewed tag, then refreshes. */
    fun batchTagTagExpenses(ids: List<Int>, tagId: Int, add: Boolean, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                apiBatchTagExpenses(ids, tagId, add)
                refreshTagExpenses()
            } catch (e: Exception) {
                onError(e.message ?: "Operation failed")
            }
        }
    }

    /** Deletes the selected expenses of the viewed tag, then refreshes the list and totals. */
    fun batchDeleteTagExpenses(ids: List<Int>, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                apiBatchDeleteExpenses(ids)
                refreshTagExpenses()
            } catch (e: Exception) {
                onError(e.message ?: "Delete failed")
            }
        }
    }

    /**
     * Reloads the currently-viewed tag's expenses in a single request (keeping the loaded count
     * stable so scroll position is preserved) and refreshes the tag totals. Call after an expense
     * linked to the viewed tag is created/edited/deleted so the tag detail view stays in sync.
     */
    fun refreshTagExpenses() {
        val tagId = selectedTag?.id ?: return
        viewModelScope.launch {
            isLoadingExpenses = true
            try {
                val loadedCount = _tagExpenses.value.size.coerceAtLeast(tagExpensePageSize)
                val refreshed = apiListExpenses(0, loadedCount, tagId = tagId)
                _tagExpenses.value = refreshed
                tagExpenseHasMore = refreshed.size == loadedCount
                tagExpensePage = loadedCount / tagExpensePageSize
                fetchTags()
            } catch (_: Exception) {
            } finally {
                isLoadingExpenses = false
            }
        }
    }

    fun saveTag(
        label: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val tagIn = TagIn(label = label.trim())
                if (selectedTag != null) {
                    apiUpdateTag(selectedTag!!.id, tagIn)
                } else {
                    apiCreateTag(tagIn)
                }
                fetchTags()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Save failed")
            }
        }
    }

    fun deleteTag(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                apiDeleteTag(selectedTag!!.id)
                fetchTags()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Delete failed")
            }
        }
    }
}
