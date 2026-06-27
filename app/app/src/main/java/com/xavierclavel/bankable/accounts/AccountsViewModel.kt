package com.xavierclavel.bankable.accounts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.bankable.api.apiCreateAccount
import com.xavierclavel.bankable.api.apiCreateAccountReport
import com.xavierclavel.bankable.api.apiDeleteAccount
import com.xavierclavel.bankable.api.apiDeleteAccountReport
import com.xavierclavel.bankable.api.apiGetAccountTrendsMonth
import com.xavierclavel.bankable.api.apiGetAccountTrendsYear
import com.xavierclavel.bankable.api.apiGetUserTrendsMonth
import com.xavierclavel.bankable.api.apiGetUserTrendsYear
import com.xavierclavel.bankable.api.apiListAccountReports
import com.xavierclavel.bankable.api.apiListAccounts
import com.xavierclavel.bankable.api.apiUpdateAccount
import com.xavierclavel.bankable.api.apiUpdateAccountReport
import com.xavierclavel.bankable.model.AccountIn
import com.xavierclavel.bankable.model.AccountOut
import com.xavierclavel.bankable.model.AccountReportIn
import com.xavierclavel.bankable.model.AccountReportOut
import com.xavierclavel.bankable.model.AccountTrendDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AccountsViewModel : ViewModel() {

    private val _accounts = MutableStateFlow<List<AccountOut>>(emptyList())
    val accounts: StateFlow<List<AccountOut>> = _accounts

    private val _reports = MutableStateFlow<List<AccountReportOut>>(emptyList())
    val reports: StateFlow<List<AccountReportOut>> = _reports

    private val _trends = MutableStateFlow<List<AccountTrendDto>>(emptyList())
    val trends: StateFlow<List<AccountTrendDto>> = _trends

    var selectedAccount by mutableStateOf<AccountOut?>(null)
        private set
    var selectedReport by mutableStateOf<AccountReportOut?>(null)
        private set
    var timescale by mutableStateOf("month")
        private set
    var chartDisplay by mutableStateOf("value")
        private set
    var isLoading by mutableStateOf(false)
        private set

    init {
        loadAccounts()
    }

    // ── Accounts ──────────────────────────────────────────────────────────────

    private suspend fun fetchAccounts() {
        _accounts.value = apiListAccounts()
    }

    fun loadAccounts() {
        viewModelScope.launch {
            isLoading = true
            try { fetchAccounts() } catch (_: Exception) { } finally { isLoading = false }
        }
    }

    fun prepareNewAccount() { selectedAccount = null }

    fun prepareEditAccount(account: AccountOut) { selectedAccount = account }

    fun saveAccount(name: String, type: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (selectedAccount != null) {
                    apiUpdateAccount(selectedAccount!!.id, AccountIn(name, type))
                } else {
                    apiCreateAccount(AccountIn(name, type))
                }
                fetchAccounts()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Save failed")
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                apiDeleteAccount(selectedAccount!!.id)
                fetchAccounts()
                selectedAccount = null
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Delete failed")
            }
        }
    }

    // ── Account selection ─────────────────────────────────────────────────────

    fun selectAccount(account: AccountOut) {
        selectedAccount = account
        loadReports()
        loadTrends(account.id)
    }

    // ── Account reports ───────────────────────────────────────────────────────

    private suspend fun fetchReports() {
        val id = selectedAccount?.id ?: return
        _reports.value = apiListAccountReports(id, 0, 100)
    }

    fun loadReports() {
        viewModelScope.launch {
            try { fetchReports() } catch (_: Exception) {}
        }
    }

    fun prepareNewReport() { selectedReport = null }

    fun prepareEditReport(report: AccountReportOut) { selectedReport = report }

    fun saveReport(amount: String, date: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val reportIn = AccountReportIn(amount, date)
                if (selectedReport != null) {
                    apiUpdateAccountReport(selectedReport!!.id, reportIn)
                } else {
                    apiCreateAccountReport(selectedAccount!!.id, reportIn)
                }
                fetchReports()
                fetchAccounts()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Save failed")
            }
        }
    }

    fun deleteReport(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                apiDeleteAccountReport(selectedReport!!.id)
                fetchReports()
                fetchAccounts()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Delete failed")
            }
        }
    }

    // ── Trends / charts ───────────────────────────────────────────────────────

    fun loadTrends(accountId: Int? = selectedAccount?.id) {
        viewModelScope.launch {
            try {
                _trends.value = when {
                    accountId != null && timescale == "month" -> apiGetAccountTrendsMonth(accountId)
                    accountId != null -> apiGetAccountTrendsYear(accountId)
                    timescale == "month" -> apiGetUserTrendsMonth()
                    else -> apiGetUserTrendsYear()
                }
            } catch (_: Exception) {}
        }
    }

    @JvmName("updateTimescale")
    fun setTimescale(value: String, accountId: Int? = selectedAccount?.id) {
        timescale = value
        loadTrends(accountId)
    }

    @JvmName("updateChartDisplay")
    fun setChartDisplay(value: String) { chartDisplay = value }
}
