package com.xavierclavel.bankable.summary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.bankable.data.MonthlyTotal
import com.xavierclavel.bankable.data.Reports
import com.xavierclavel.bankable.data.Sync
import com.xavierclavel.bankable.data.currentMonthIndex
import com.xavierclavel.bankable.data.monthIndex
import com.xavierclavel.bankable.model.SubcategoryOutlierDto
import com.xavierclavel.bankable.model.SummaryDto
import com.xavierclavel.bankable.util.currentMonth
import com.xavierclavel.bankable.util.currentYear
import kotlin.jvm.JvmName
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// How far above its average a subcategory must run, over a year, to be flagged...
const val OUTLIER_THRESHOLD = 0.5
// ...provided its year totals at least this much...
const val OUTLIER_MIN_TOTAL = 250
// ...with the average taken over this many years either side of it.
const val OUTLIER_YEARS_AROUND = 2

/** The summary of a month or a year, computed from the local expenses: it follows every change. */
class SummaryViewModel : ViewModel() {

    var summary by mutableStateOf<SummaryDto?>(null)
        private set
    var outliers by mutableStateOf<List<SubcategoryOutlierDto>>(emptyList())
        private set
    // True while the first expenses are still being downloaded.
    var isLoading by mutableStateOf(false)
        private set

    var selectedYear by mutableIntStateOf(currentYear())
        private set
    var selectedMonth by mutableIntStateOf(currentMonth())
        private set
    var timescale by mutableStateOf("month")
        private set
    var selectedType by mutableStateOf("EXPENSE")
        private set

    // The first month with an expense: the earliest period that can be shown.
    private var oldestMonth by mutableIntStateOf(currentMonthIndex())

    private var totals: List<MonthlyTotal> = emptyList()

    init {
        viewModelScope.launch {
            combine(Reports.observeMonthlyTotals(), Sync.isSyncing) { totals, syncing -> totals to syncing }
                .collect { (newTotals, syncing) ->
                    totals = newTotals
                    oldestMonth = Reports.firstMonth(newTotals) ?: currentMonthIndex()
                    isLoading = syncing && newTotals.isEmpty()
                    compute()
                }
        }
    }

    private fun compute() {
        val (first, last) = if (timescale == "month") {
            monthIndex(selectedYear, selectedMonth).let { it to it }
        } else {
            monthIndex(selectedYear, 1) to monthIndex(selectedYear, 12)
        }
        summary = Reports.summary(totals, first, last).copy(
            year = selectedYear,
            month = selectedMonth.takeIf { timescale == "month" },
        )
        outliers = if (timescale == "year") {
            Reports.outliers(
                totals = totals,
                year = selectedYear,
                threshold = OUTLIER_THRESHOLD,
                minTotalCents = OUTLIER_MIN_TOTAL * 100L,
                yearsAround = OUTLIER_YEARS_AROUND,
            )
        } else {
            emptyList()
        }
    }

    @JvmName("updateTimescale")
    fun setTimescale(value: String) {
        timescale = value
        compute()
    }

    @JvmName("updateSelectedType")
    fun setSelectedType(value: String) { selectedType = value }

    fun previousPeriod() {
        if (!canGoBack()) return
        if (timescale == "month") {
            if (selectedMonth == 1) { selectedMonth = 12; selectedYear-- }
            else selectedMonth--
        } else {
            selectedYear--
        }
        compute()
    }

    fun nextPeriod() {
        if (!canGoForward()) return
        if (timescale == "month") {
            if (selectedMonth == 12) { selectedMonth = 1; selectedYear++ }
            else selectedMonth++
        } else {
            selectedYear++
        }
        compute()
    }

    // The years the outliers' average spans: cut short by the first year on record and by
    // the current one, as Reports.outliers does.
    fun outlierWindow(): IntRange =
        maxOf(selectedYear - OUTLIER_YEARS_AROUND, oldestMonth / 12)..minOf(selectedYear + OUTLIER_YEARS_AROUND, currentYear())

    fun canGoBack(): Boolean = if (timescale == "month")
        monthIndex(selectedYear, selectedMonth) > oldestMonth
    else
        selectedYear > oldestMonth / 12

    fun canGoForward(): Boolean {
        val curYear = currentYear()
        val curMonth = currentMonth()
        return if (timescale == "month")
            !(selectedYear == curYear && selectedMonth >= curMonth)
        else
            selectedYear < curYear
    }
}
