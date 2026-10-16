package com.xavierclavel.bankable.categories

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.bankable.data.CategoryStore
import com.xavierclavel.bankable.data.ExpenseStore
import com.xavierclavel.bankable.data.Sync
import com.xavierclavel.bankable.model.CategoryOut
import com.xavierclavel.bankable.model.ExpenseOut
import com.xavierclavel.bankable.model.SubcategoryOut
import com.xavierclavel.bankable.util.userMessage
import kotlin.jvm.JvmName
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

private const val EXPENSE_PAGE_SIZE = 50

/** Which expenses a detail view lists: those of [id] (a category or a subcategory), the first [limit]. */
private data class ExpensePage(val id: String, val limit: Int)

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModel : ViewModel() {

    val categories: StateFlow<List<CategoryOut>> = CategoryStore.observe()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val categoryExpensePage = MutableStateFlow<ExpensePage?>(null)
    private val subcategoryExpensePage = MutableStateFlow<ExpensePage?>(null)

    // Expenses belonging to the category currently being viewed (paginated).
    val categoryExpenses: StateFlow<List<ExpenseOut>> = categoryExpensePage
        .flatMapLatest { page ->
            if (page == null) flowOf(emptyList()) else ExpenseStore.observe(categoryId = page.id, limit = page.limit)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Expenses belonging to the subcategory currently being viewed (paginated).
    val subcategoryExpenses: StateFlow<List<ExpenseOut>> = subcategoryExpensePage
        .flatMapLatest { page ->
            if (page == null) flowOf(emptyList()) else ExpenseStore.observe(subcategoryId = page.id, limit = page.limit)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    var selectedCategory by mutableStateOf<CategoryOut?>(null)
        private set
    var selectedSubcategory by mutableStateOf<SubcategoryOut?>(null)
        private set
    var pickerCategory by mutableStateOf<CategoryOut?>(null)
        private set
    var selectedColor by mutableStateOf<String?>(null)
        private set
    var selectedIcon by mutableStateOf<String?>(null)
        private set
    var typeFilter by mutableStateOf("EXPENSE")
        private set
    // True while the first categories / expenses are still being downloaded.
    var isLoading by mutableStateOf(false)
        private set
    var isLoadingExpenses by mutableStateOf(false)
        private set
    var isLoadingSubcategoryExpenses by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            combine(Sync.isSyncing, categories) { syncing, list -> syncing && list.isEmpty() }
                .collect { isLoading = it }
        }
        viewModelScope.launch {
            combine(Sync.isSyncing, categoryExpenses) { syncing, list -> syncing && list.isEmpty() }
                .collect { isLoadingExpenses = it }
        }
        viewModelScope.launch {
            combine(Sync.isSyncing, subcategoryExpenses) { syncing, list -> syncing && list.isEmpty() }
                .collect { isLoadingSubcategoryExpenses = it }
        }
    }

    @JvmName("updateTypeFilter")
    fun setTypeFilter(type: String) { typeFilter = type }
    @JvmName("updateSelectedColor")
    fun setSelectedColor(color: String?) { selectedColor = color }
    @JvmName("updateSelectedIcon")
    fun setSelectedIcon(icon: String?) { selectedIcon = icon }
    @JvmName("updatePickerCategory")
    fun setPickerCategory(category: CategoryOut) { pickerCategory = category }

    fun prepareNewCategory() {
        selectedCategory = null
        selectedSubcategory = null
        selectedColor = null
        selectedIcon = null
    }

    fun prepareEditCategory(category: CategoryOut) {
        selectedCategory = category
        selectedSubcategory = null
        selectedColor = category.color
        selectedIcon = category.icon
    }

    /** Opens the category detail view, listing its expenses from the first page. */
    fun prepareViewCategory(category: CategoryOut) {
        selectedCategory = category
        categoryExpensePage.value = ExpensePage(category.id, EXPENSE_PAGE_SIZE)
    }

    fun loadMoreCategoryExpenses() {
        val page = categoryExpensePage.value ?: return
        if (categoryExpenses.value.size >= page.limit) {
            categoryExpensePage.update { it?.copy(limit = it.limit + EXPENSE_PAGE_SIZE) }
        }
    }

    fun batchTagCategoryExpenses(ids: List<String>, tagId: String, add: Boolean) {
        viewModelScope.launch {
            try { ExpenseStore.setTag(ids, tagId, add) } catch (_: Exception) {}
        }
    }

    fun batchDeleteCategoryExpenses(ids: List<String>) {
        viewModelScope.launch {
            try { ExpenseStore.delete(ids) } catch (_: Exception) {}
        }
    }

    /** Opens the subcategory detail view, listing its expenses from the first page. */
    fun prepareViewSubcategory(subcategory: SubcategoryOut) {
        selectedSubcategory = subcategory
        subcategoryExpensePage.value = ExpensePage(subcategory.id, EXPENSE_PAGE_SIZE)
    }

    fun loadMoreSubcategoryExpenses() {
        val page = subcategoryExpensePage.value ?: return
        if (subcategoryExpenses.value.size >= page.limit) {
            subcategoryExpensePage.update { it?.copy(limit = it.limit + EXPENSE_PAGE_SIZE) }
        }
    }

    fun batchTagSubcategoryExpenses(ids: List<String>, tagId: String, add: Boolean) {
        viewModelScope.launch {
            try { ExpenseStore.setTag(ids, tagId, add) } catch (_: Exception) {}
        }
    }

    fun batchDeleteSubcategoryExpenses(ids: List<String>) {
        viewModelScope.launch {
            try { ExpenseStore.delete(ids) } catch (_: Exception) {}
        }
    }

    fun prepareNewSubcategory(parentCategory: CategoryOut) {
        selectedSubcategory = null
        selectedIcon = null
        pickerCategory = parentCategory
    }

    fun prepareEditSubcategory(subcategory: SubcategoryOut) {
        selectedSubcategory = subcategory
        selectedIcon = subcategory.icon
        pickerCategory = categories.value.find { cat ->
            cat.subcategories.any { it.id == subcategory.id }
        }
    }

    fun saveCategory(
        name: String,
        type: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                CategoryStore.saveCategory(
                    id = selectedCategory?.id,
                    name = name,
                    type = selectedCategory?.type ?: type,
                    color = selectedColor ?: "",
                    icon = selectedIcon ?: "unknown",
                )
                onSuccess()
            } catch (e: Exception) {
                onError(userMessage(e, "Save failed"))
            }
        }
    }

    fun deleteCategory(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                CategoryStore.deleteCategory(selectedCategory!!.id)
                onSuccess()
            } catch (e: Exception) {
                onError(userMessage(e, "Delete failed"))
            }
        }
    }

    fun saveSubcategory(
        name: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                CategoryStore.saveSubcategory(
                    id = selectedSubcategory?.id,
                    categoryId = pickerCategory!!.id,
                    name = name,
                    icon = selectedIcon ?: "unknown",
                    type = pickerCategory?.type ?: "EXPENSE",
                )
                onSuccess()
            } catch (e: Exception) {
                onError(userMessage(e, "Save failed"))
            }
        }
    }

    fun deleteSubcategory(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                CategoryStore.deleteSubcategory(selectedSubcategory!!.id)
                onSuccess()
            } catch (e: Exception) {
                onError(userMessage(e, "Delete failed"))
            }
        }
    }
}
