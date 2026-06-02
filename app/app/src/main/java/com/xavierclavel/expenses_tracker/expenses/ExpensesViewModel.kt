package com.xavierclavel.expenses_tracker.expenses

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.expenses_tracker.api.apiCreateExpense
import com.xavierclavel.expenses_tracker.api.apiDeleteExpense
import com.xavierclavel.expenses_tracker.api.apiListExpenses
import com.xavierclavel.expenses_tracker.api.apiUpdateExpense
import com.xavierclavel.expenses_tracker.model.ExpenseIn
import com.xavierclavel.expenses_tracker.model.ExpenseOut
import com.xavierclavel.expenses_tracker.model.SubcategoryOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
            val newExpenses = apiListExpenses(0, pageSize)
            _expenses.value = newExpenses
            hasMore = newExpenses.size == pageSize
            currentPage = 1
        } finally {
            isLoading = false
        }
    }

    private fun loadExpenses() {
        if (isLoading || !hasMore) return
        viewModelScope.launch {
            isLoading = true
            try {
                val newExpenses = apiListExpenses(currentPage, pageSize)
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
