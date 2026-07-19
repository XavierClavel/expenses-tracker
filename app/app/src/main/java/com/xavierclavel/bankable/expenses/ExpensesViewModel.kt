package com.xavierclavel.bankable.expenses

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.bankable.api.apiBatchDeleteExpenses
import com.xavierclavel.bankable.api.apiBatchTagExpenses
import com.xavierclavel.bankable.api.apiCreateExpense
import com.xavierclavel.bankable.api.apiDeleteExpense
import com.xavierclavel.bankable.api.apiListExpenses
import com.xavierclavel.bankable.api.apiUpdateExpense
import com.xavierclavel.bankable.model.ExpenseIn
import com.xavierclavel.bankable.model.ExpenseOut
import com.xavierclavel.bankable.model.SubcategoryOut
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Structured filters applied to the expense list, in addition to the free-text [ExpensesViewModel.searchQuery].
 * A null field means "no constraint". [type] is "EXPENSE", "INCOME", or null for both.
 */
data class ExpenseFilter(
    val categoryId: Int? = null,
    val subcategoryId: Int? = null,
    val type: String? = null,
    val from: String? = null,
    val to: String? = null,
    val minAmount: String? = null,
    val maxAmount: String? = null,
    val tagId: Int? = null,
) {
    val isActive: Boolean
        get() = categoryId != null || subcategoryId != null || type != null ||
            from != null || to != null || minAmount != null || maxAmount != null ||
            tagId != null
}

class ExpensesViewModel : ViewModel() {

    private val _expenses = MutableStateFlow<List<ExpenseOut>>(emptyList())
    val expenses: StateFlow<List<ExpenseOut>> = _expenses

    var selectedExpense by mutableStateOf<ExpenseOut?>(null)
        private set
    var selectedSubcategory by mutableStateOf<SubcategoryOut?>(null)
        private set
    var selectedType by mutableStateOf("EXPENSE")
        private set
    // Tag ids assigned to the expense currently being created/edited.
    var selectedTagIds by mutableStateOf<Set<Int>>(emptySet())
        private set
    var isLoading by mutableStateOf(false)
        private set

    // --- Multi-select / batch mode (expense list) ---
    var selectionMode by mutableStateOf(false)
        private set
    var selectedExpenseIds by mutableStateOf<Set<Int>>(emptySet())
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

    private var currentPage = 0
    private var hasMore = true
    private val pageSize = 50

    init {
        refresh()
    }

    private suspend fun fetchExpenses() {
        currentPage = 0
        hasMore = true
        isLoading = true
        try {
            val newExpenses = fetchPage(0)
            _expenses.value = newExpenses
            hasMore = newExpenses.size == pageSize
            currentPage = 1
        } finally {
            isLoading = false
        }
    }

    /**
     * Re-fetches all expenses currently loaded (every page fetched so far) in a single request,
     * instead of collapsing back to the first page. Keeps the list length stable after an
     * edit/delete so the LazyColumn restores the user's scroll position.
     */
    private suspend fun reloadLoadedExpenses() {
        val loadedCount = _expenses.value.size.coerceAtLeast(pageSize)
        isLoading = true
        try {
            val refreshed = fetchPage(page = 0, size = loadedCount)
            _expenses.value = refreshed
            hasMore = refreshed.size == loadedCount
            currentPage = loadedCount / pageSize
        } finally {
            isLoading = false
        }
    }

    private suspend fun fetchPage(page: Int, size: Int = pageSize): List<ExpenseOut> =
        apiListExpenses(
            page = page,
            size = size,
            categoryId = filter.categoryId,
            subcategoryId = filter.subcategoryId,
            type = filter.type,
            from = filter.from,
            to = filter.to,
            query = searchQuery.takeIf { it.isNotBlank() },
            minAmount = filter.minAmount,
            maxAmount = filter.maxAmount,
            tagId = filter.tagId,
        )

    private fun loadExpenses() {
        if (isLoading || !hasMore) return
        viewModelScope.launch {
            isLoading = true
            try {
                val newExpenses = fetchPage(currentPage)
                _expenses.value = _expenses.value + newExpenses
                hasMore = newExpenses.size == pageSize
                currentPage++
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMore() {
        if (!isLoading && hasMore) loadExpenses()
    }

    fun refresh() {
        viewModelScope.launch {
            try { fetchExpenses() } catch (_: Exception) {}
        }
    }

    /** Updates the live search text and refetches after a short debounce. */
    @JvmName("updateSearchQuery")
    fun setSearchQuery(query: String) {
        searchQuery = query
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(300)
            try { fetchExpenses() } catch (_: Exception) {}
        }
    }

    /** Replaces the structured filters and refetches immediately. */
    fun applyFilter(newFilter: ExpenseFilter) {
        filter = newFilter
        refresh()
    }

    /** Clears the search query and all filters, then refetches. */
    fun clearFilters() {
        searchDebounceJob?.cancel()
        searchQuery = ""
        filter = ExpenseFilter()
        refresh()
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
    fun toggleTag(tagId: Int) {
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
                val expenseIn = ExpenseIn(
                    title = title,
                    amount = amount,
                    currency = "EUR",
                    date = date,
                    categoryId = selectedSubcategory?.id,
                    type = selectedType,
                    tagIds = selectedTagIds.toList(),
                )
                if (selectedExpense != null) {
                    apiUpdateExpense(selectedExpense!!.id, expenseIn)
                } else {
                    apiCreateExpense(expenseIn)
                }
                reloadLoadedExpenses()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Save failed")
            }
        }
    }

    /** Enters batch-selection mode with [expenseId] as the first selected item. */
    fun enterSelectionMode(expenseId: Int) {
        selectionMode = true
        selectedExpenseIds = setOf(expenseId)
    }

    /** Toggles an expense in the selection; leaving the selection empty exits the mode. */
    fun toggleSelection(expenseId: Int) {
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

    /** Adds or removes [tagId] on every currently selected expense, then refreshes and exits. */
    fun batchTagSelection(tagId: Int, add: Boolean, onError: (String) -> Unit) {
        val ids = selectedExpenseIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                apiBatchTagExpenses(ids, tagId, add)
                reloadLoadedExpenses()
                clearSelection()
            } catch (e: Exception) {
                onError(e.message ?: "Operation failed")
            }
        }
    }

    /** Deletes every currently selected expense, then refreshes and exits selection. */
    fun batchDeleteSelection(onError: (String) -> Unit) {
        val ids = selectedExpenseIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                apiBatchDeleteExpenses(ids)
                reloadLoadedExpenses()
                clearSelection()
            } catch (e: Exception) {
                onError(e.message ?: "Delete failed")
            }
        }
    }

    fun deleteExpense(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                apiDeleteExpense(selectedExpense!!.id)
                reloadLoadedExpenses()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Delete failed")
            }
        }
    }
}
