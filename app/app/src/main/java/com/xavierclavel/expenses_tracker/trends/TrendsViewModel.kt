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

data class BarGroup(
    val label: String,
    val income: Float,
    val expense: Float,
)

class TrendsViewModel : ViewModel() {

    // "income_expense" | "flow" | "category"
    var dataMode by mutableStateOf("income_expense")
        private set
    var timescale by mutableStateOf("month")
        private set
    var aggregation by mutableStateOf("total")
        private set

    // Active selection for "category" mode — subcategory takes priority over category
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

    @JvmName("updateDataMode")
    fun setDataMode(value: String) {
        dataMode = value
        if (value != "category") {
            selectedCategory    = null
            selectedSubcategory = null
        }
        load()
    }

    @JvmName("updateTimescale")
    fun setTimescale(value: String) { timescale = value; load() }

    @JvmName("updateAggregation")
    fun setAggregation(value: String) { aggregation = value; load() }

    fun selectCategory(cat: CategoryOut) {
        selectedCategory    = cat
        selectedSubcategory = null
        load()
    }

    fun selectSubcategory(sub: SubcategoryOut) {
        selectedSubcategory = sub
        selectedCategory    = null
        load()
    }

    fun clearCategorySelection() {
        selectedCategory    = null
        selectedSubcategory = null
        _bars.value         = emptyList()
        _groups.value       = emptyList()
    }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            try {
                when (dataMode) {
                    "income_expense" -> loadIncomeExpense()
                    "flow"           -> loadFlow()
                    "category"       -> loadCategoryMode()
                }
            } catch (_: Exception) {
                _bars.value   = emptyList()
                _groups.value = emptyList()
            } finally {
                isLoading = false
            }
        }
    }

    // ── Private loaders ───────────────────────────────────────────────────────

    private suspend fun loadIncomeExpense() {
        if (timescale == "month") {
            _groups.value = apiGetMonthTrends().map { t ->
                BarGroup(monthLabel(t.year, t.month ?: 1), t.totalIncome.toFloatOrNull() ?: 0f, t.totalExpenses.toFloatOrNull() ?: 0f)
            }
        } else {
            _groups.value = apiGetYearTrends().map { t ->
                BarGroup(t.year.toString(), agg(t.totalIncome, t.meanIncome, t.medianIncome), agg(t.totalExpenses, t.meanExpenses, t.medianExpenses))
            }
        }
        _bars.value = emptyList()
    }

    private suspend fun loadFlow() {
        if (timescale == "month") {
            _bars.value = apiGetMonthTrends().map { t ->
                val v = (t.totalIncome.toFloatOrNull() ?: 0f) - (t.totalExpenses.toFloatOrNull() ?: 0f)
                BarEntry(v, monthLabel(t.year, t.month ?: 1))
            }
        } else {
            _bars.value = apiGetYearFlowTrends().map { t ->
                BarEntry(aggCat(t), t.year.toString())
            }
        }
        _groups.value = emptyList()
    }

    private suspend fun loadCategoryMode() {
        val sub = selectedSubcategory
        val cat = selectedCategory
        // Expenses are negated so they show as red bars (negative values)
        val sign = if ((sub?.type ?: cat?.type) == "EXPENSE") -1f else 1f

        when {
            sub != null -> {
                _bars.value = if (timescale == "month")
                    apiGetMonthSubcategoryTrends(sub.id).map { t -> BarEntry((t.total.toFloatOrNull() ?: 0f) * sign, monthLabel(t.year, t.month ?: 1)) }
                else
                    apiGetYearSubcategoryTrends(sub.id).map { t -> BarEntry(aggCat(t) * sign, t.year.toString()) }
            }
            cat != null -> {
                _bars.value = if (timescale == "month")
                    apiGetMonthCategoryTrends(cat.id).map { t -> BarEntry((t.total.toFloatOrNull() ?: 0f) * sign, monthLabel(t.year, t.month ?: 1)) }
                else
                    apiGetYearCategoryTrends(cat.id).map { t -> BarEntry(aggCat(t) * sign, t.year.toString()) }
            }
            else -> _bars.value = emptyList()
        }
        _groups.value = emptyList()
    }

    // ── Aggregation helpers ───────────────────────────────────────────────────

    private fun agg(total: String, mean: String?, median: String?) = when (aggregation) {
        "average" -> mean?.toFloatOrNull()   ?: 0f
        "median"  -> median?.toFloatOrNull() ?: 0f
        else      -> total.toFloatOrNull()   ?: 0f
    }

    private fun aggCat(t: com.xavierclavel.expenses_tracker.model.CategoryTrendDto) = when (aggregation) {
        "average" -> t.average?.toFloatOrNull() ?: 0f
        "median"  -> t.median?.toFloatOrNull()  ?: 0f
        else      -> t.total.toFloatOrNull()   ?: 0f
    }

    private fun monthLabel(year: Int, month: Int): String {
        val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val name = months.getOrElse(month - 1) { month.toString() }
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return if (year == currentYear) name else "$name '${year % 100}"
    }
}
