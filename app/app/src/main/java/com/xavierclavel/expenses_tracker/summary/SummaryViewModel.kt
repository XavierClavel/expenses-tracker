package com.xavierclavel.expenses_tracker.summary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.expenses_tracker.api.apiGetMonthSummary
import com.xavierclavel.expenses_tracker.api.apiGetOldestExpenseDate
import com.xavierclavel.expenses_tracker.api.apiGetYearSummary
import com.xavierclavel.expenses_tracker.model.SummaryDto
import kotlinx.coroutines.launch
import java.util.Calendar

class SummaryViewModel : ViewModel() {

    var summary by mutableStateOf<SummaryDto?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set

    private val now = Calendar.getInstance()
    var selectedYear by mutableIntStateOf(now.get(Calendar.YEAR))
        private set
    var selectedMonth by mutableIntStateOf(now.get(Calendar.MONTH) + 1)
        private set
    var timescale by mutableStateOf("month")
        private set
    var selectedType by mutableStateOf("EXPENSE")
        private set

    private var oldestYear = 2000
    private var oldestMonth = 1

    init {
        viewModelScope.launch {
            try {
                val oldest = apiGetOldestExpenseDate()
                if (oldest != null) {
                    val parts = oldest.split("-")
                    oldestYear = parts.getOrNull(0)?.toIntOrNull() ?: 2000
                    oldestMonth = parts.getOrNull(1)?.toIntOrNull() ?: 1
                }
            } catch (_: Exception) {}
            loadSummary()
        }
    }

    fun refresh() = loadSummary()

    private fun loadSummary() {
        viewModelScope.launch {
            isLoading = true
            try {
                summary = if (timescale == "month") {
                    apiGetMonthSummary(selectedYear, selectedMonth)
                } else {
                    apiGetYearSummary(selectedYear)
                }
            } catch (_: Exception) {
                summary = null
            } finally {
                isLoading = false
            }
        }
    }

    @JvmName("updateTimescale")
    fun setTimescale(value: String) {
        timescale = value
        loadSummary()
    }

    @JvmName("updateSelectedType")
    fun setSelectedType(value: String) { selectedType = value }

    fun previousPeriod() {
        if (timescale == "month") {
            if (selectedYear == oldestYear && selectedMonth <= oldestMonth) return
            if (selectedMonth == 1) { selectedMonth = 12; selectedYear-- }
            else selectedMonth--
        } else {
            if (selectedYear <= oldestYear) return
            selectedYear--
        }
        loadSummary()
    }

    fun nextPeriod() {
        val curYear = now.get(Calendar.YEAR)
        val curMonth = now.get(Calendar.MONTH) + 1
        if (timescale == "month") {
            if (selectedYear == curYear && selectedMonth >= curMonth) return
            if (selectedMonth == 12) { selectedMonth = 1; selectedYear++ }
            else selectedMonth++
        } else {
            if (selectedYear >= curYear) return
            selectedYear++
        }
        loadSummary()
    }

    fun canGoBack(): Boolean = if (timescale == "month")
        !(selectedYear == oldestYear && selectedMonth <= oldestMonth)
    else
        selectedYear > oldestYear

    fun canGoForward(): Boolean {
        val curYear = now.get(Calendar.YEAR)
        val curMonth = now.get(Calendar.MONTH) + 1
        return if (timescale == "month")
            !(selectedYear == curYear && selectedMonth >= curMonth)
        else
            selectedYear < curYear
    }
}
