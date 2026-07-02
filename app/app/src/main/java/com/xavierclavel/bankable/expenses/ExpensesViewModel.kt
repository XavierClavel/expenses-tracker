package com.xavierclavel.bankable.expenses

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) {
    val isActive: Boolean
        get() = categoryId != null || subcategoryId != null || type != null ||
            from != null || to != null || minAmount != null || maxAmount != null
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
    var isLoading by mutableStateOf(false)
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

    private suspend fun fetchPage(page: Int): List<ExpenseOut> =
        apiListExpenses(
            page = page,
            size = pageSize,
            categoryId = filter.categoryId,
            subcategoryId = filter.subcategoryId,
            type = filter.type,
            from = filter.from,
            to = filter.to,
            query = searchQuery.takeIf { it.isNotBlank() },
            minAmount = filter.minAmount,
            maxAmount = filter.maxAmount,
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
    }

    fun prepareEditExpense(expense: ExpenseOut, subcategory: SubcategoryOut?) {
        selectedExpense = expense
        selectedSubcategory = subcategory
        selectedType = expense.type
    }

    @JvmName("updateSelectedSubcategory")
    fun setSelectedSubcategory(sub: SubcategoryOut?) { selectedSubcategory = sub }

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
                )
                if (selectedExpense != null) {
                    apiUpdateExpense(selectedExpense!!.id, expenseIn)
                } else {
                    apiCreateExpense(expenseIn)
                }
                fetchExpenses()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Save failed")
            }
        }
    }

    fun deleteExpense(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                apiDeleteExpense(selectedExpense!!.id)
                fetchExpenses()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Delete failed")
            }
        }
    }
}
