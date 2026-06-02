package com.xavierclavel.expenses_tracker.trends

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.expenses_tracker.accounts.BarEntry
import com.xavierclavel.expenses_tracker.api.apiGetMonthCategoryTrends
import com.xavierclavel.expenses_tracker.api.apiGetMonthSubcategoryTrends
import com.xavierclavel.expenses_tracker.api.apiGetMonthTrends
import com.xavierclavel.expenses_tracker.api.apiGetYearCategoryTrends
import com.xavierclavel.expenses_tracker.api.apiGetYearFlowTrends
import com.xavierclavel.expenses_tracker.api.apiGetYearSubcategoryTrends
import com.xavierclavel.expenses_tracker.api.apiGetYearTrends
import com.xavierclavel.expenses_tracker.model.CategoryOut
import com.xavierclavel.expenses_tracker.model.SubcategoryOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

private val COLOR_INCOME  = androidx.compose.ui.graphics.Color(0xFF4CAF50)
private val COLOR_EXPENSE = androidx.compose.ui.graphics.Color(0xFFE53935)

data class BarGroup(
    val label: String,
    val income: Float,
    val expense: Float,
)

class TrendsViewModel : ViewModel() {

    var dataType by mutableStateOf("income_expense")
        private set
    var timescale by mutableStateOf("month")
        private set
    var aggregation by mutableStateOf("total")
        private set
    var selectedCategory by mutableStateOf<CategoryOut?>(null)
        private set
    var selectedSubcategory by mutableStateOf<SubcategoryOut?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set

    private val _bars   = MutableStateFlow<List<BarEntry>>(emptyList())
    val bars: StateFlow<List<BarEntry>> = _bars

    private val _groups = MutableStateFlow<List<BarGroup>>(emptyList())
    val groups: StateFlow<List<BarGroup>> = _groups

    private var categories: List<CategoryOut> = emptyList()

    fun updateCategories(newCategories: List<CategoryOut>) {
        if (categories == newCategories) return
        categories = newCategories
        load()
    }

    @JvmName("updateDataType")
    fun setDataType(value: String) {
        dataType = value
        // Auto-clear selections that don't match the new type filter
        val requiredType = when (value) {
            "category_in", "subcategory_in" -> "INCOME"
            "category_out", "subcategory_out" -> "EXPENSE"
            else -> null
        }
        if (requiredType != null) {
            if (selectedCategory?.type != requiredType) selectedCategory = null
            if (selectedSubcategory?.type != requiredType) selectedSubcategory = null
        }
        load()
    }

    @JvmName("updateTimescale")
    fun setTimescale(value: String) { timescale = value; load() }

    @JvmName("updateAggregation")
    fun setAggregation(value: String) { aggregation = value; load() }

    @JvmName("updateSelectedCategory")
    fun setSelectedCategory(cat: CategoryOut?) { selectedCategory = cat; load() }

    @JvmName("updateSelectedSubcategory")
    fun setSelectedSubcategory(sub: SubcategoryOut?) { selectedSubcategory = sub; load() }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            try {
                when (dataType) {
                    "income_expense" -> loadIncomeExpense()
                    "flow"           -> loadFlow()
                    "category_in", "category_out"       -> loadCategory()
                    "subcategory_in", "subcategory_out"  -> loadSubcategory()
                }
            } catch (_: Exception) {
                _bars.value = emptyList()
                _groups.value = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    private suspend fun loadIncomeExpense() {
        if (timescale == "month") {
            val data = apiGetMonthTrends()
            _groups.value = data.map { t ->
                BarGroup(
                    label   = monthLabel(t.year, t.month ?: 1),
                    income  = t.totalIncome.toFloatOrNull() ?: 0f,
                    expense = t.totalExpenses.toFloatOrNull() ?: 0f,
                )
            }
        } else {
            val data = apiGetYearTrends()
            _groups.value = data.map { t ->
                BarGroup(
                    label   = t.year.toString(),
                    income  = agg(t.totalIncome, t.meanIncome, t.medianIncome),
                    expense = agg(t.totalExpenses, t.meanExpenses, t.medianExpenses),
                )
            }
        }
        _bars.value = emptyList()
    }

    private suspend fun loadFlow() {
        if (timescale == "month") {
            val data = apiGetMonthTrends()
            _bars.value = data.map { t ->
                val v = (t.totalIncome.toFloatOrNull() ?: 0f) - (t.totalExpenses.toFloatOrNull() ?: 0f)
                BarEntry(value = v, label = monthLabel(t.year, t.month ?: 1))
            }
        } else {
            val data = apiGetYearFlowTrends()
            _bars.value = data.map { t ->
                BarEntry(value = aggCat(t), label = t.year.toString())
            }
        }
        _groups.value = emptyList()
    }

    private suspend fun loadCategory() {
        val cat = selectedCategory ?: run { _bars.value = emptyList(); _groups.value = emptyList(); return }
        val color = categories.find { it.id == cat.id }?.color ?: cat.color
        if (timescale == "month") {
            val data = apiGetMonthCategoryTrends(cat.id)
            _bars.value = data.map { t ->
                BarEntry(value = t.total.toFloatOrNull() ?: 0f, label = monthLabel(t.year, t.month ?: 1))
            }
        } else {
            val data = apiGetYearCategoryTrends(cat.id)
            _bars.value = data.map { t ->
                BarEntry(value = aggCat(t), label = t.year.toString())
            }
        }
        _groups.value = emptyList()
    }

    private suspend fun loadSubcategory() {
        val sub = selectedSubcategory ?: run { _bars.value = emptyList(); _groups.value = emptyList(); return }
        if (timescale == "month") {
            val data = apiGetMonthSubcategoryTrends(sub.id)
            _bars.value = data.map { t ->
                BarEntry(value = t.total.toFloatOrNull() ?: 0f, label = monthLabel(t.year, t.month ?: 1))
            }
        } else {
            val data = apiGetYearSubcategoryTrends(sub.id)
            _bars.value = data.map { t ->
                BarEntry(value = aggCat(t), label = t.year.toString())
            }
        }
        _groups.value = emptyList()
    }

    private fun agg(total: String, mean: String?, median: String?) = when (aggregation) {
        "average" -> mean?.toFloatOrNull() ?: 0f
        "median"  -> median?.toFloatOrNull() ?: 0f
        else      -> total.toFloatOrNull() ?: 0f
    }

    private fun aggCat(t: com.xavierclavel.expenses_tracker.model.CategoryTrendDto) = when (aggregation) {
        "average" -> t.average.toFloatOrNull() ?: 0f
        "median"  -> t.median.toFloatOrNull() ?: 0f
        else      -> t.total.toFloatOrNull() ?: 0f
    }

    private fun monthLabel(year: Int, month: Int): String {
        val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val name = months.getOrElse(month - 1) { month.toString() }
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return if (year == currentYear) name else "$name '${year % 100}"
    }
}
