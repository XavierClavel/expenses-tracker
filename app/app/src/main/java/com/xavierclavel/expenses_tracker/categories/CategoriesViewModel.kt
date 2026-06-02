package com.xavierclavel.expenses_tracker.categories

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.expenses_tracker.api.apiCreateCategory
import com.xavierclavel.expenses_tracker.api.apiCreateSubcategory
import com.xavierclavel.expenses_tracker.api.apiDeleteCategory
import com.xavierclavel.expenses_tracker.api.apiDeleteSubcategory
import com.xavierclavel.expenses_tracker.api.apiListCategories
import com.xavierclavel.expenses_tracker.api.apiUpdateCategory
import com.xavierclavel.expenses_tracker.api.apiUpdateSubcategory
import com.xavierclavel.expenses_tracker.model.CategoryIn
import com.xavierclavel.expenses_tracker.model.CategoryOut
import com.xavierclavel.expenses_tracker.model.SubcategoryIn
import com.xavierclavel.expenses_tracker.model.SubcategoryOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CategoriesViewModel : ViewModel() {

    private val _categories = MutableStateFlow<List<CategoryOut>>(emptyList())
    val categories: StateFlow<List<CategoryOut>> = _categories

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
