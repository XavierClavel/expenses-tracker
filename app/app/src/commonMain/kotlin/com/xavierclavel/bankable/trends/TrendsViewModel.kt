package com.xavierclavel.bankable.trends

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.bankable.accounts.BarEntry
import com.xavierclavel.bankable.data.MonthlyTotal
import com.xavierclavel.bankable.data.Reports
import com.xavierclavel.bankable.data.Sync
import com.xavierclavel.bankable.model.CategoryOut
import com.xavierclavel.bankable.model.CategoryTrendDto
import com.xavierclavel.bankable.model.SubcategoryOut
import com.xavierclavel.bankable.platform.platformDefaultLocaleTag
import com.xavierclavel.bankable.platform.platformShortMonthName
import com.xavierclavel.bankable.util.currentYear
import kotlin.jvm.JvmName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

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
    private var locale: String = platformDefaultLocaleTag()
    private var totals: List<MonthlyTotal> = emptyList()

    init {
        // Trends are computed from the local expenses: they follow every change.
        viewModelScope.launch {
            combine(Reports.observeMonthlyTotals(), Sync.isSyncing) { totals, syncing -> totals to syncing }
                .collect { (newTotals, syncing) ->
                    totals = newTotals
                    isLoading = syncing && newTotals.isEmpty()
                    load()
                }
        }
    }

    fun updateLocale(newLocale: String) {
        if (locale == newLocale) return
        locale = newLocale
        load()
    }

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
        when (dataMode) {
            "income_expense" -> loadIncomeExpense()
            "flow"           -> loadFlow()
            "category"       -> loadCategoryMode()
        }
    }

    // ── Private loaders ───────────────────────────────────────────────────────

    private fun loadIncomeExpense() {
        if (timescale == "month") {
            _groups.value = Reports.incomeExpenseByMonth(totals).map { t ->
                BarGroup(monthLabel(t.year, t.month ?: 1), t.totalIncome.toFloatOrNull() ?: 0f, t.totalExpenses.toFloatOrNull() ?: 0f)
            }
        } else {
            _groups.value = Reports.incomeExpenseByYear(totals).map { t ->
                BarGroup(t.year.toString(), agg(t.totalIncome, t.meanIncome, t.medianIncome), agg(t.totalExpenses, t.meanExpenses, t.medianExpenses))
            }
        }
        _bars.value = emptyList()
    }

    private fun loadFlow() {
        if (timescale == "month") {
            _bars.value = Reports.incomeExpenseByMonth(totals).map { t ->
                val v = (t.totalIncome.toFloatOrNull() ?: 0f) - (t.totalExpenses.toFloatOrNull() ?: 0f)
                BarEntry(v, monthLabel(t.year, t.month ?: 1))
            }
        } else {
            _bars.value = Reports.flowByYear(totals).map { t ->
                BarEntry(aggCat(t), t.year.toString())
            }
        }
        _groups.value = emptyList()
    }

    private fun loadCategoryMode() {
        val sub = selectedSubcategory
        val cat = selectedCategory
        val matching: ((MonthlyTotal) -> Boolean)? = when {
            sub != null -> { row -> row.subcategoryId == sub.id }
            cat != null -> { row -> row.categoryId == cat.id }
            else -> null
        }
        // Category mode always shows positive (unsigned) values, colored by the
        // category accent — no income/expense sign.
        _bars.value = when {
            matching == null -> emptyList()
            timescale == "month" -> Reports.trendByMonth(totals, matching)
                .map { t -> BarEntry(t.total.toFloatOrNull() ?: 0f, monthLabel(t.year, t.month ?: 1)) }
            else -> Reports.trendByYear(totals, matching).map { t -> BarEntry(aggCat(t), t.year.toString()) }
        }
        _groups.value = emptyList()
    }

    // ── Aggregation helpers ───────────────────────────────────────────────────

    private fun agg(total: String, mean: String?, median: String?) = when (aggregation) {
        "average" -> mean?.toFloatOrNull()   ?: 0f
        "median"  -> median?.toFloatOrNull() ?: 0f
        else      -> total.toFloatOrNull()   ?: 0f
    }

    private fun aggCat(t: CategoryTrendDto) = when (aggregation) {
        "average" -> t.average?.toFloatOrNull() ?: 0f
        "median"  -> t.median?.toFloatOrNull()  ?: 0f
        else      -> t.total.toFloatOrNull()   ?: 0f
    }

    private fun monthLabel(year: Int, month: Int): String {
        val name = platformShortMonthName(year, month, locale).replaceFirstChar { it.titlecase() }
        return if (year == currentYear()) name else "$name '${year % 100}"
    }
}
