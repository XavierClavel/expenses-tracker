package com.xavierclavel.bankable.accounts

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.bankable.api.apiBatchDeleteAccountReports
import com.xavierclavel.bankable.api.apiBatchDeleteInvestments
import com.xavierclavel.bankable.api.apiCreateAccount
import com.xavierclavel.bankable.api.apiCreateAccountReport
import com.xavierclavel.bankable.api.apiCreateInvestment
import com.xavierclavel.bankable.api.apiDeleteAccount
import com.xavierclavel.bankable.api.apiDeleteAccountReport
import com.xavierclavel.bankable.api.apiDeleteInvestment
import com.xavierclavel.bankable.api.apiGetAccountTrendsMonth
import com.xavierclavel.bankable.api.apiGetAccountTrendsYear
import com.xavierclavel.bankable.api.apiGetUserTrendsMonth
import com.xavierclavel.bankable.api.apiGetUserTrendsYear
import com.xavierclavel.bankable.api.apiListAccountReports
import com.xavierclavel.bankable.api.apiListAccounts
import com.xavierclavel.bankable.api.apiListInvestments
import com.xavierclavel.bankable.api.apiUpdateAccount
import com.xavierclavel.bankable.api.apiUpdateAccountReport
import com.xavierclavel.bankable.api.apiUpdateInvestment
import com.xavierclavel.bankable.model.AccountIn
import com.xavierclavel.bankable.model.AccountOut
import com.xavierclavel.bankable.model.AccountReportIn
import com.xavierclavel.bankable.model.AccountReportOut
import com.xavierclavel.bankable.model.AccountTrendDto
import com.xavierclavel.bankable.model.InvestmentIn
import com.xavierclavel.bankable.model.InvestmentOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AccountsViewModel : ViewModel() {

    private val _accounts = MutableStateFlow<List<AccountOut>>(emptyList())
    val accounts: StateFlow<List<AccountOut>> = _accounts

    private val _reports = MutableStateFlow<List<AccountReportOut>>(emptyList())
    val reports: StateFlow<List<AccountReportOut>> = _reports

    private val _transfers = MutableStateFlow<List<InvestmentOut>>(emptyList())
    val transfers: StateFlow<List<InvestmentOut>> = _transfers

    private val _trends = MutableStateFlow<List<AccountTrendDto>>(emptyList())
    val trends: StateFlow<List<AccountTrendDto>> = _trends

    // All-accounts yearly trends, kept in sync with the accounts list so the total
    // header can show the current year's interest across every account.
    private val _userYearTrends = MutableStateFlow<List<AccountTrendDto>>(emptyList())
    val userYearTrends: StateFlow<List<AccountTrendDto>> = _userYearTrends

    var selectedAccount by mutableStateOf<AccountOut?>(null)
        private set
    var selectedReport by mutableStateOf<AccountReportOut?>(null)
        private set
    var selectedTransfer by mutableStateOf<InvestmentOut?>(null)
        private set
    var timescale by mutableStateOf("month")
        private set
    var chartDisplay by mutableStateOf("value")
        private set
    // Which part of the balance the chart shows: "both" (stacked contributions +
    // interests), "transfers" (contributions only), or "interests" only.
    var chartSource by mutableStateOf("both")
        private set
    var isLoading by mutableStateOf(false)
        private set

    init {
        loadAccounts()
    }

    // ── Accounts ──────────────────────────────────────────────────────────────

    private suspend fun fetchAccounts() {
        _accounts.value = apiListAccounts()
        try { _userYearTrends.value = apiGetUserTrendsYear() } catch (_: Exception) {}
    }

    fun loadAccounts() {
        viewModelScope.launch {
            isLoading = true
            try { fetchAccounts() } catch (_: Exception) { } finally { isLoading = false }
        }
    }

    fun prepareNewAccount() { selectedAccount = null }

    fun prepareEditAccount(account: AccountOut) { selectedAccount = account }

    fun saveAccount(name: String, type: String, tracking: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                if (selectedAccount != null) {
                    apiUpdateAccount(selectedAccount!!.id, AccountIn(name, type, tracking))
                } else {
                    apiCreateAccount(AccountIn(name, type, tracking))
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
        loadTransfers()
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

    /** Deletes several account reports at once, then refreshes reports and balances. */
    fun batchDeleteReports(ids: List<Int>, onError: (String) -> Unit) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                apiBatchDeleteAccountReports(ids)
                fetchReports()
                fetchAccounts()
            } catch (e: Exception) {
                onError(e.message ?: "Delete failed")
            }
        }
    }

    // ── Transfers (money added / retrieved) ─────────────────────────────────────

    private suspend fun fetchTransfers() {
        val id = selectedAccount?.id ?: return
        _transfers.value = apiListInvestments(id)
    }

    fun loadTransfers() {
        viewModelScope.launch {
            try { fetchTransfers() } catch (_: Exception) {}
        }
    }

    fun prepareNewTransfer() { selectedTransfer = null }

    fun prepareEditTransfer(transfer: InvestmentOut) { selectedTransfer = transfer }

    fun saveTransfer(amount: String, type: String, date: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val accountId = selectedAccount?.id ?: return@launch
                val investmentIn = InvestmentIn(
                    amount = amount,
                    accountId = accountId.toLong(),
                    type = type,
                    date = date,
                )
                if (selectedTransfer != null) {
                    apiUpdateInvestment(selectedTransfer!!.id, investmentIn)
                } else {
                    apiCreateInvestment(accountId, investmentIn)
                }
                fetchTransfers()
                fetchAccounts()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Save failed")
            }
        }
    }

    fun deleteTransfer(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                apiDeleteInvestment(selectedTransfer!!.id)
                fetchTransfers()
                fetchAccounts()
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Delete failed")
            }
        }
    }

    /** Deletes several transfers at once, then refreshes transfers and balances. */
    fun batchDeleteTransfers(ids: List<Long>, onError: (String) -> Unit) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            try {
                apiBatchDeleteInvestments(ids)
                fetchTransfers()
                fetchAccounts()
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

    @JvmName("updateChartSource")
    fun setChartSource(value: String) { chartSource = value }
}
