package com.xavierclavel.bankable.tags

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.bankable.data.ExpenseStore
import com.xavierclavel.bankable.data.Sync
import com.xavierclavel.bankable.data.TagStore
import com.xavierclavel.bankable.model.ExpenseOut
import com.xavierclavel.bankable.model.TagOut
import com.xavierclavel.bankable.util.userMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG_EXPENSE_PAGE_SIZE = 50

/** Which expenses the tag detail view lists: those linked to [tagId], the first [limit]. */
private data class TagExpensePage(val tagId: String, val limit: Int)

@OptIn(ExperimentalCoroutinesApi::class)
class TagsViewModel : ViewModel() {

    val tags: StateFlow<List<TagOut>> = TagStore.observe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val tagExpensePage = MutableStateFlow<TagExpensePage?>(null)

    // Expenses linked to the tag currently being viewed (paginated).
    val tagExpenses: StateFlow<List<ExpenseOut>> = tagExpensePage
        .flatMapLatest { page ->
            if (page == null) flowOf(emptyList()) else ExpenseStore.observe(tagId = page.tagId, limit = page.limit)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var selectedTag by mutableStateOf<TagOut?>(null)
        private set
    // True while the first tags / expenses are still being downloaded.
    var isLoading by mutableStateOf(false)
        private set
    var isLoadingExpenses by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            combine(Sync.isSyncing, tags) { syncing, list -> syncing && list.isEmpty() }
                .collect { isLoading = it }
        }
        viewModelScope.launch {
            combine(Sync.isSyncing, tagExpenses) { syncing, list -> syncing && list.isEmpty() }
                .collect { isLoadingExpenses = it }
        }
    }

    /** Fetches the changes made on other devices. */
    fun loadTags() {
        Sync.requestSync()
    }

    fun prepareNewTag() {
        selectedTag = null
    }

    fun prepareEditTag(tag: TagOut) {
        selectedTag = tag
    }

    /** Opens the tag detail view, listing its expenses from the first page. */
    fun prepareViewTag(tag: TagOut) {
        selectedTag = tag
        tagExpensePage.value = TagExpensePage(tag.id, TAG_EXPENSE_PAGE_SIZE)
    }

    fun loadMoreTagExpenses() {
        val page = tagExpensePage.value ?: return
        if (tagExpenses.value.size >= page.limit) {
            tagExpensePage.update { it?.copy(limit = it.limit + TAG_EXPENSE_PAGE_SIZE) }
        }
    }

    /** Adds or removes [tagId] on the selected expenses of the viewed tag. */
    fun batchTagTagExpenses(ids: List<String>, tagId: String, add: Boolean, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                ExpenseStore.setTag(ids, tagId, add)
            } catch (e: Exception) {
                onError(userMessage(e, "Operation failed"))
            }
        }
    }

    /** Deletes the selected expenses of the viewed tag. */
    fun batchDeleteTagExpenses(ids: List<String>, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                ExpenseStore.delete(ids)
            } catch (e: Exception) {
                onError(userMessage(e, "Delete failed"))
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
                TagStore.save(id = selectedTag?.id, label = label.trim())
                onSuccess()
            } catch (e: Exception) {
                onError(userMessage(e, "Save failed"))
            }
        }
    }

    fun deleteTag(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                TagStore.delete(selectedTag!!.id)
                onSuccess()
            } catch (e: Exception) {
                onError(userMessage(e, "Delete failed"))
            }
        }
    }
}
