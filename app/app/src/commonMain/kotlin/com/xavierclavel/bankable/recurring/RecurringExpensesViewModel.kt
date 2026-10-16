package com.xavierclavel.bankable.recurring

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.bankable.api.apiCreateRecurringExpense
import com.xavierclavel.bankable.api.apiDeleteRecurringExpense
import com.xavierclavel.bankable.api.apiListRecurringExpenses
import com.xavierclavel.bankable.api.apiUpdateRecurringExpense
import com.xavierclavel.bankable.data.SyncType
import com.xavierclavel.bankable.data.localTagIds
import com.xavierclavel.bankable.data.requireServerIds
import com.xavierclavel.bankable.model.RecurringExpenseIn
import com.xavierclavel.bankable.model.RecurringExpenseOut
import com.xavierclavel.bankable.model.SubcategoryOut
import com.xavierclavel.bankable.util.userMessage
import kotlin.jvm.JvmName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecurringExpensesViewModel : ViewModel() {

    private val _recurringExpenses = MutableStateFlow<List<RecurringExpenseOut>>(emptyList())
    val recurringExpenses: StateFlow<List<RecurringExpenseOut>> = _recurringExpenses

    var selectedRecurringExpense by mutableStateOf<RecurringExpenseOut?>(null)
        private set
    var selectedSubcategory by mutableStateOf<SubcategoryOut?>(null)
        private set
    var selectedType by mutableStateOf("EXPENSE")
        private set
    // Tags (local ids) copied onto every expense the recurring expense being edited creates.
    var selectedTagIds by mutableStateOf<Set<String>>(emptySet())
        private set
    var isLoading by mutableStateOf(false)
        private set

    private suspend fun fetchRecurringExpenses() {
        _recurringExpenses.value = apiListRecurringExpenses()
    }

    fun loadRecurringExpenses() {
        viewModelScope.launch {
            isLoading = true
            try {
                fetchRecurringExpenses()
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    fun prepareNewRecurringExpense() {
        selectedRecurringExpense = null
        selectedSubcategory = null
        selectedType = "EXPENSE"
        selectedTagIds = emptySet()
    }

    fun prepareEditRecurringExpense(recurringExpense: RecurringExpenseOut, subcategory: SubcategoryOut?) {
        selectedRecurringExpense = recurringExpense
        selectedSubcategory = subcategory
        selectedType = recurringExpense.type
        selectedTagIds = emptySet()
        viewModelScope.launch { selectedTagIds = localTagIds(recurringExpense.tagIds) }
    }

    @JvmName("updateSelectedSubcategory")
    fun setSelectedSubcategory(sub: SubcategoryOut?) { selectedSubcategory = sub }

    @JvmName("updateSelectedType")
    fun setSelectedType(type: String) {
        selectedType = type
        selectedSubcategory = null
    }

    /** Adds or removes [tagId] from the set assigned to the recurring expense being edited. */
    fun toggleTag(tagId: String) {
        selectedTagIds = if (selectedTagIds.contains(tagId)) {
            selectedTagIds - tagId
        } else {
            selectedTagIds + tagId
        }
    }

    /**
     * Creates or updates the selected recurring expense. [onSuccess] receives true when the
     * backend created an expense on save, which happens when [dayOfMonth] is today: callers
     * use it to refresh the expense list.
     *
     * Recurring expenses live on the server only, and reference the subcategory and tags by
     * their server ids: ones created offline must have been synced first.
     */
    fun saveRecurringExpense(
        title: String,
        amount: String,
        dayOfMonth: Int,
        onSuccess: (createdExpense: Boolean) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val recurringExpenseIn = RecurringExpenseIn(
                    title = title,
                    amount = amount,
                    currency = "EUR",
                    categoryId = selectedSubcategory
                        ?.let { requireServerIds(SyncType.SUBCATEGORY, listOf(it.id)).single() },
                    type = selectedType,
                    tagIds = requireServerIds(SyncType.TAG, selectedTagIds.toList()),
                    dayOfMonth = dayOfMonth,
                )
                val previous = selectedRecurringExpense
                val saved = if (previous != null) {
                    apiUpdateRecurringExpense(previous.id, recurringExpenseIn)
                } else {
                    apiCreateRecurringExpense(recurringExpenseIn)
                }
                fetchRecurringExpenses()
                onSuccess(saved.lastGeneratedDate != previous?.lastGeneratedDate)
            } catch (e: Exception) {
                onError(userMessage(e, "Save failed"))
            }
        }
    }

    fun deleteRecurringExpense(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                apiDeleteRecurringExpense(selectedRecurringExpense!!.id)
                fetchRecurringExpenses()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Delete failed")
            }
        }
    }
}
