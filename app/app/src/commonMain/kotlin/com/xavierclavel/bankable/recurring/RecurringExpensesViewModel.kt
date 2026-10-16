package com.xavierclavel.bankable.recurring

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.bankable.data.RecurringExpenseStore
import com.xavierclavel.bankable.data.Sync
import com.xavierclavel.bankable.model.RecurringExpenseOut
import com.xavierclavel.bankable.model.SubcategoryOut
import com.xavierclavel.bankable.resources.Res
import com.xavierclavel.bankable.resources.error_invalid_amount
import com.xavierclavel.bankable.util.parseCents
import com.xavierclavel.bankable.util.userMessage
import kotlin.jvm.JvmName
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

class RecurringExpensesViewModel : ViewModel() {

    val recurringExpenses: StateFlow<List<RecurringExpenseOut>> = RecurringExpenseStore.observe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var selectedRecurringExpense by mutableStateOf<RecurringExpenseOut?>(null)
        private set
    var selectedSubcategory by mutableStateOf<SubcategoryOut?>(null)
        private set
    var selectedType by mutableStateOf("EXPENSE")
        private set
    // Tags copied onto every expense the recurring expense being edited creates.
    var selectedTagIds by mutableStateOf<Set<String>>(emptySet())
        private set
    // True while the first recurring expenses are still being downloaded.
    var isLoading by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            combine(Sync.isSyncing, recurringExpenses) { syncing, list -> syncing && list.isEmpty() }
                .collect { isLoading = it }
        }
    }

    /** Fetches the changes made elsewhere, such as the next dates moving as expenses get created. */
    fun loadRecurringExpenses() {
        Sync.requestSync()
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
        selectedTagIds = recurringExpense.tagIds.toSet()
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
     * Creates or updates the selected recurring expense. [onSuccess] receives true when a new
     * one is due today: the server creates its expense once the app syncs.
     */
    fun saveRecurringExpense(
        title: String,
        amount: String,
        dayOfMonth: Int,
        onSuccess: (dueToday: Boolean) -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val cents = parseCents(amount)
                if (cents == null) {
                    onError(getString(Res.string.error_invalid_amount))
                    return@launch
                }
                val dueToday = RecurringExpenseStore.save(
                    id = selectedRecurringExpense?.id,
                    title = title,
                    amountCents = cents,
                    currency = "EUR",
                    type = selectedType,
                    subcategoryId = selectedSubcategory?.id,
                    tagIds = selectedTagIds,
                    dayOfMonth = dayOfMonth,
                )
                onSuccess(dueToday)
            } catch (e: Exception) {
                onError(userMessage(e, "Save failed"))
            }
        }
    }

    fun deleteRecurringExpense(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                RecurringExpenseStore.delete(selectedRecurringExpense!!.id)
                onSuccess()
            } catch (e: Exception) {
                onError(userMessage(e, "Delete failed"))
            }
        }
    }
}
