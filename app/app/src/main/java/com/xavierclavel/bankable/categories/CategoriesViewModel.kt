package com.xavierclavel.bankable.categories

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.bankable.api.apiBatchDeleteExpenses
import com.xavierclavel.bankable.api.apiBatchTagExpenses
import com.xavierclavel.bankable.api.apiCreateCategory
import com.xavierclavel.bankable.api.apiCreateSubcategory
import com.xavierclavel.bankable.api.apiDeleteCategory
import com.xavierclavel.bankable.api.apiDeleteSubcategory
import com.xavierclavel.bankable.api.apiListCategories
import com.xavierclavel.bankable.api.apiListExpenses
import com.xavierclavel.bankable.api.apiUpdateCategory
import com.xavierclavel.bankable.api.apiUpdateSubcategory
import com.xavierclavel.bankable.model.CategoryIn
import com.xavierclavel.bankable.model.CategoryOut
import com.xavierclavel.bankable.model.ExpenseOut
import com.xavierclavel.bankable.model.SubcategoryIn
import com.xavierclavel.bankable.model.SubcategoryOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CategoriesViewModel : ViewModel() {

    private val _categories = MutableStateFlow<List<CategoryOut>>(emptyList())
    val categories: StateFlow<List<CategoryOut>> = _categories

    // Expenses belonging to the category currently being viewed (paginated).
    private val _categoryExpenses = MutableStateFlow<List<ExpenseOut>>(emptyList())
    val categoryExpenses: StateFlow<List<ExpenseOut>> = _categoryExpenses

    // Expenses belonging to the subcategory currently being viewed (paginated).
    private val _subcategoryExpenses = MutableStateFlow<List<ExpenseOut>>(emptyList())
    val subcategoryExpenses: StateFlow<List<ExpenseOut>> = _subcategoryExpenses

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
    var isLoading by mutableStateOf(false)
        private set
    var isLoadingExpenses by mutableStateOf(false)
        private set
    var isLoadingSubcategoryExpenses by mutableStateOf(false)
        private set

    private var expensePage = 0
    private var expenseHasMore = true
    private var subExpensePage = 0
    private var subExpenseHasMore = true
    private val expensePageSize = 50

    init {
        loadCategories()
    }

    private suspend fun fetchCategories() {
        _categories.value = apiListCategories()
    }

    fun loadCategories() {
        viewModelScope.launch {
            isLoading = true
            try {
                fetchCategories()
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
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

    /** Opens the category detail view and (re)loads its expense history from the first page. */
    fun prepareViewCategory(category: CategoryOut) {
        selectedCategory = category
        _categoryExpenses.value = emptyList()
        expensePage = 0
        expenseHasMore = true
        loadCategoryExpenses()
    }

    private fun loadCategoryExpenses() {
        val categoryId = selectedCategory?.id ?: return
        if (isLoadingExpenses || !expenseHasMore) return
        viewModelScope.launch {
            isLoadingExpenses = true
            try {
                val newExpenses = apiListExpenses(expensePage, expensePageSize, categoryId = categoryId)
                _categoryExpenses.value =
                    if (expensePage == 0) newExpenses else _categoryExpenses.value + newExpenses
                expenseHasMore = newExpenses.size == expensePageSize
                expensePage++
            } catch (_: Exception) {
            } finally {
                isLoadingExpenses = false
            }
        }
    }

    fun loadMoreCategoryExpenses() {
        if (!isLoadingExpenses && expenseHasMore) loadCategoryExpenses()
    }

    /** Refetches the currently-loaded category expenses in one request (stable count). */
    private suspend fun reloadCategoryExpenses() {
        val categoryId = selectedCategory?.id ?: return
        val count = _categoryExpenses.value.size.coerceAtLeast(expensePageSize)
        val refreshed = apiListExpenses(0, count, categoryId = categoryId)
        _categoryExpenses.value = refreshed
        expenseHasMore = refreshed.size == count
        expensePage = count / expensePageSize
    }

    fun batchTagCategoryExpenses(ids: List<Int>, tagId: Int, add: Boolean) {
        viewModelScope.launch {
            try {
                apiBatchTagExpenses(ids, tagId, add)
                reloadCategoryExpenses()
            } catch (_: Exception) {}
        }
    }

    fun batchDeleteCategoryExpenses(ids: List<Int>) {
        viewModelScope.launch {
            try {
                apiBatchDeleteExpenses(ids)
                reloadCategoryExpenses()
                fetchCategories()
            } catch (_: Exception) {}
        }
    }

    /** Opens the subcategory detail view and (re)loads its expense history from the first page. */
    fun prepareViewSubcategory(subcategory: SubcategoryOut) {
        selectedSubcategory = subcategory
        _subcategoryExpenses.value = emptyList()
        subExpensePage = 0
        subExpenseHasMore = true
        loadSubcategoryExpenses()
    }

    private fun loadSubcategoryExpenses() {
        val subcategoryId = selectedSubcategory?.id ?: return
        if (isLoadingSubcategoryExpenses || !subExpenseHasMore) return
        viewModelScope.launch {
            isLoadingSubcategoryExpenses = true
            try {
                val newExpenses = apiListExpenses(subExpensePage, expensePageSize, subcategoryId = subcategoryId)
                _subcategoryExpenses.value =
                    if (subExpensePage == 0) newExpenses else _subcategoryExpenses.value + newExpenses
                subExpenseHasMore = newExpenses.size == expensePageSize
                subExpensePage++
            } catch (_: Exception) {
            } finally {
                isLoadingSubcategoryExpenses = false
            }
        }
    }

    fun loadMoreSubcategoryExpenses() {
        if (!isLoadingSubcategoryExpenses && subExpenseHasMore) loadSubcategoryExpenses()
    }

    /** Refetches the currently-loaded subcategory expenses in one request (stable count). */
    private suspend fun reloadSubcategoryExpenses() {
        val subcategoryId = selectedSubcategory?.id ?: return
        val count = _subcategoryExpenses.value.size.coerceAtLeast(expensePageSize)
        val refreshed = apiListExpenses(0, count, subcategoryId = subcategoryId)
        _subcategoryExpenses.value = refreshed
        subExpenseHasMore = refreshed.size == count
        subExpensePage = count / expensePageSize
    }

    fun batchTagSubcategoryExpenses(ids: List<Int>, tagId: Int, add: Boolean) {
        viewModelScope.launch {
            try {
                apiBatchTagExpenses(ids, tagId, add)
                reloadSubcategoryExpenses()
            } catch (_: Exception) {}
        }
    }

    fun batchDeleteSubcategoryExpenses(ids: List<Int>) {
        viewModelScope.launch {
            try {
                apiBatchDeleteExpenses(ids)
                reloadSubcategoryExpenses()
                fetchCategories()
            } catch (_: Exception) {}
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
        pickerCategory = _categories.value.find { cat ->
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
                val categoryIn = CategoryIn(
                    name = name,
                    type = selectedCategory?.type ?: type,
                    color = selectedColor ?: "",
                    icon = selectedIcon ?: "unknown",
                )
                if (selectedCategory != null) {
                    apiUpdateCategory(selectedCategory!!.id, categoryIn)
                } else {
                    apiCreateCategory(categoryIn)
                }
                fetchCategories()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Save failed")
            }
        }
    }

    fun deleteCategory(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                apiDeleteCategory(selectedCategory!!.id)
                fetchCategories()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Delete failed")
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
                val subcategoryIn = SubcategoryIn(
                    name = name,
                    type = pickerCategory?.type ?: "EXPENSE",
                    icon = selectedIcon ?: "unknown",
                    parentCategory = pickerCategory!!.id,
                )
                if (selectedSubcategory != null) {
                    apiUpdateSubcategory(selectedSubcategory!!.id, subcategoryIn)
                } else {
                    apiCreateSubcategory(subcategoryIn)
                }
                fetchCategories()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Save failed")
            }
        }
    }

    fun deleteSubcategory(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                apiDeleteSubcategory(selectedSubcategory!!.id)
                fetchCategories()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Delete failed")
            }
        }
    }
}
