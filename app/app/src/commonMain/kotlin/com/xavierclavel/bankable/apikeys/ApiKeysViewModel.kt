package com.xavierclavel.bankable.apikeys

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.bankable.api.apiCreateApiKey
import com.xavierclavel.bankable.api.apiDeleteApiKey
import com.xavierclavel.bankable.api.apiListApiKeys
import com.xavierclavel.bankable.model.ApiKeyOut
import com.xavierclavel.bankable.model.CreatedApiKeyOut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ApiKeysViewModel : ViewModel() {

    private val _apiKeys = MutableStateFlow<List<ApiKeyOut>>(emptyList())
    val apiKeys: StateFlow<List<ApiKeyOut>> = _apiKeys

    var isLoading by mutableStateOf(false)
        private set
    var isCreating by mutableStateOf(false)
        private set

    /**
     * The key just created, secret included. The backend hands the secret out only once, so it
     * is kept just while the screen shows it, and dropped by [clearCreatedApiKey].
     */
    var createdApiKey by mutableStateOf<CreatedApiKeyOut?>(null)
        private set

    private suspend fun fetchApiKeys() {
        _apiKeys.value = apiListApiKeys()
    }

    fun loadApiKeys() {
        viewModelScope.launch {
            isLoading = true
            try {
                fetchApiKeys()
            } catch (_: Exception) {
            } finally {
                isLoading = false
            }
        }
    }

    fun createApiKey(
        name: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (isCreating) return
        viewModelScope.launch {
            isCreating = true
            try {
                val created = apiCreateApiKey(name.trim())
                createdApiKey = created
                // Appended locally rather than refetched, so a failing refetch can't be reported
                // as a failed creation while the one-time secret is lost. The list is oldest first.
                _apiKeys.value = _apiKeys.value + created.apiKey
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Create failed")
            } finally {
                isCreating = false
            }
        }
    }

    fun clearCreatedApiKey() {
        createdApiKey = null
    }

    fun revokeApiKey(apiKey: ApiKeyOut, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                apiDeleteApiKey(apiKey.id)
                _apiKeys.value = _apiKeys.value.filterNot { it.id == apiKey.id }
            } catch (e: Exception) {
                onError(e.message ?: "Revoke failed")
            }
        }
    }
}
