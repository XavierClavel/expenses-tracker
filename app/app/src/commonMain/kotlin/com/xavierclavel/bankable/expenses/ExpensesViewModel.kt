package com.xavierclavel.bankable.expenses

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.bankable.data.ExpenseStore
import com.xavierclavel.bankable.data.RecurringExpenseStore
import com.xavierclavel.bankable.data.Sync
import com.xavierclavel.bankable.model.ExpenseOut
import com.xavierclavel.bankable.model.SubcategoryOut
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.error_invalid_amount
import com.xavierclavel.bankable.util.parseCents
import com.xavierclavel.bankable.util.userMessage
import kotlin.jvm.JvmName
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/**
 * Structured filters applied to the expense list, in addition to the free-text [ExpensesViewModel.searchQuery].
 * A null field means "no constraint". [type] is "EXPENSE", "INCOME", or null for both.
 */
data class ExpenseFilter(
    val categoryId: String? = null,
    val subcategoryId: String? = null,
    val type: String? = null,
    val from: String? = null,
    val to: String? = null,
    val minAmount: String? = null,
    val maxAmount: String? = null,
    val tagId: String? = null,
) {
    val isActive: Boolean
        get() = categoryId != null || subcategoryId != null || type != null ||
            from != null || to != null || minAmount != null || maxAmount != null ||
            tagId != null
}

private const val PAGE_SIZE = 50

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModel : ViewModel() {

    /** What the list shows: the filters, the search text, and how many expenses are loaded. */
    private data class ListQuery(val filter: ExpenseFilter, val search: String, val limit: Int)

    private val listQuery = MutableStateFlow(ListQuery(ExpenseFilter(), "", PAGE_SIZE))

    val expenses: StateFlow<List<ExpenseOut>> = listQuery
        .flatMapLatest { query ->
            ExpenseStore.observe(
                categoryId = query.filter.categoryId,
                subcategoryId = query.filter.subcategoryId,
                type = query.filter.type,
                from = query.filter.from,
                to = query.filter.to,
                query = query.search,
                minCents = query.filter.minAmount?.let { parseCents(it) },
                maxCents = query.filter.maxAmount?.let { parseCents(it) },
                tagId = query.filter.tagId,
                limit = query.limit,
            )
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var selectedExpense by mutableStateOf<ExpenseOut?>(null)
        private set
    var selectedSubcategory by mutableStateOf<SubcategoryOut?>(null)
        private set
    var selectedType by mutableStateOf("EXPENSE")
        private set
    // Tag ids assigned to the expense currently being created/edited.
    var selectedTagIds by mutableStateOf<Set<String>>(emptySet())
        private set
    // True while the first expenses are still being downloaded.
    var isLoading by mutableStateOf(false)
        private set

    // --- Multi-select / batch mode (expense list) ---
    var selectionMode by mutableStateOf(false)
        private set
    var selectedExpenseIds by mutableStateOf<Set<String>>(emptySet())
        private set

    // --- Search / filter state ---
    var searchQuery by mutableStateOf("")
        private set
    var filter by mutableStateOf(ExpenseFilter())
        private set

    /** True when any filter or a non-blank search query is narrowing the list. */
    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() || filter.isActive

    private var searchDebounceJob: Job? = null

    init {
        viewModelScope.launch {
            combine(Sync.isSyncing, expenses) { syncing, list -> syncing && list.isEmpty() }
                .collect { isLoading = it }
        }
        refresh()
    }

    /** Loads the next page, when the list holds all it was asked for. */
    fun loadMore() {
        val query = listQuery.value
        if (expenses.value.size >= query.limit) listQuery.update { it.copy(limit = it.limit + PAGE_SIZE) }
    }

    /** Fetches the changes made on other devices. */
    fun refresh() {
        Sync.requestSync()
    }

    /** Updates the live search text and filters the list after a short debounce. */
    @JvmName("updateSearchQuery")
    fun setSearchQuery(query: String) {
        searchQuery = query
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(300)
            listQuery.update { it.copy(search = query, limit = PAGE_SIZE) }
        }
    }

    /** Replaces the structured filters. */
    fun applyFilter(newFilter: ExpenseFilter) {
        filter = newFilter
        listQuery.update { it.copy(filter = newFilter, limit = PAGE_SIZE) }
    }

    /** Clears the search query and all filters. */
    fun clearFilters() {
        searchDebounceJob?.cancel()
        searchQuery = ""
        filter = ExpenseFilter()
        listQuery.value = ListQuery(ExpenseFilter(), "", PAGE_SIZE)
    }

    fun prepareNewExpense() {
        selectedExpense = null
        selectedSubcategory = null
        selectedType = "EXPENSE"
        selectedTagIds = emptySet()
    }

    fun prepareEditExpense(expense: ExpenseOut, subcategory: SubcategoryOut?) {
        selectedExpense = expense
        selectedSubcategory = subcategory
        selectedType = expense.type
        selectedTagIds = expense.tagIds.toSet()
    }

    @JvmName("updateSelectedSubcategory")
    fun setSelectedSubcategory(sub: SubcategoryOut?) { selectedSubcategory = sub }

    /** Adds or removes [tagId] from the set assigned to the expense being edited. */
    fun toggleTag(tagId: String) {
        selectedTagIds = if (selectedTagIds.contains(tagId)) {
            selectedTagIds - tagId
        } else {
            selectedTagIds + tagId
        }
    }

    @JvmName("updateSelectedType")
    fun setSelectedType(type: String) {
        selectedType = type
        selectedSubcategory = null
    }

    fun saveExpense(
        title: String,
        amount: String,
        date: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val cents = parseCents(amount)
                if (cents == null) {
                    onError(getString(Res.string.error_invalid_amount))
                    return@launch
                }
                ExpenseStore.save(
                    id = selectedExpense?.id,
                    title = title,
                    amountCents = cents,
                    currency = "EUR",
                    date = date,
                    subcategoryId = selectedSubcategory?.id,
                    type = selectedType,
                    tagIds = selectedTagIds,
                )
                onSuccess()
            } catch (e: Exception) {
                onError(userMessage(e, "Save failed"))
            }
        }
    }

    /** Enters batch-selection mode with [expenseId] as the first selected item. */
    fun enterSelectionMode(expenseId: String) {
        selectionMode = true
        selectedExpenseIds = setOf(expenseId)
    }

    /** Toggles an expense in the selection; leaving the selection empty exits the mode. */
    fun toggleSelection(expenseId: String) {
        selectedExpenseIds = if (selectedExpenseIds.contains(expenseId)) {
            selectedExpenseIds - expenseId
        } else {
            selectedExpenseIds + expenseId
        }
        if (selectedExpenseIds.isEmpty()) selectionMode = false
    }

    fun clearSelection() {
        selectionMode = false
        selectedExpenseIds = emptySet()
    }

    /** Adds or removes [tagId] on every currently selected expense, then exits selection. */
    fun batchTagSelection(tagId: String, add: Boolean, onError: (String) -> Unit) {
        val ids = selectedExpenseIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                ExpenseStore.setTag(ids, tagId, add)
                clearSelection()
            } catch (e: Exception) {
                onError(userMessage(e, "Operation failed"))
            }
        }
    }

    /** Copies every currently selected expense onto [date] ("yyyy-MM-dd"), then exits selection. */
    fun batchDuplicateSelection(date: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val ids = selectedExpenseIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                ExpenseStore.duplicate(ids, date)
                clearSelection()
                onSuccess()
            } catch (e: Exception) {
                onError(userMessage(e, "Duplicate failed"))
            }
        }
    }

    /** Creates a recurring expense from every currently selected expense, then exits selection. */
    fun batchMakeRecurringSelection(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val ids = selectedExpenseIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                RecurringExpenseStore.createFromExpenses(ids)
                clearSelection()
                onSuccess()
            } catch (e: Exception) {
                onError(userMessage(e, "Operation failed"))
            }
        }
    }

    /** Deletes every currently selected expense, then exits selection. */
    fun batchDeleteSelection(onError: (String) -> Unit) {
        val ids = selectedExpenseIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                ExpenseStore.delete(ids)
                clearSelection()
            } catch (e: Exception) {
                onError(userMessage(e, "Delete failed"))
            }
        }
    }

    fun deleteExpense(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                ExpenseStore.delete(listOf(selectedExpense!!.id))
                onSuccess()
            } catch (e: Exception) {
                onError(userMessage(e, "Delete failed"))
            }
        }
    }
}
