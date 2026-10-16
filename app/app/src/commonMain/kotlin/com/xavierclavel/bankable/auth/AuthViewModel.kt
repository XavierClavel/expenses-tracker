package com.xavierclavel.bankable.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.bankable.api.apiDeleteAccount
import com.xavierclavel.bankable.api.apiLogin
import com.xavierclavel.bankable.api.apiLoginGoogle
import com.xavierclavel.bankable.api.apiSignup
import com.xavierclavel.bankable.api.clearSessionCookies
import com.xavierclavel.bankable.api.sessionToken
import com.xavierclavel.bankable.api.unauthorizedFlow
import com.xavierclavel.bankable.data.Sync
import com.xavierclavel.bankable.platform.cancelBackgroundSync
import com.xavierclavel.bankable.storage.TokenStorage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val LOGOUT_SYNC_TIMEOUT_MS = 5_000L

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    data class Unauthenticated(val error: String? = null) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val tokenStorage = TokenStorage()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    init {
        checkAuth()
        viewModelScope.launch {
            // The session is gone: changes not pushed yet can't be anymore.
            unauthorizedFlow.collect { endSession() }
        }
    }

    private fun checkAuth() {
        viewModelScope.launch {
            val token = tokenStorage.loadToken()
            if (token != null) {
                sessionToken = token
                _authState.value = AuthState.Authenticated
            } else {
                _authState.value = AuthState.Unauthenticated()
            }
        }
    }

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val token = apiLogin(email, password)
                tokenStorage.saveToken(token)
                sessionToken = token
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Unauthenticated(e.message ?: "Login failed")
            }
        }
    }

    // Reports errors via callback rather than the global auth state so the
    // initiating screen (login or signup) stays mounted and can show them.
    fun loginWithGoogle(idToken: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val token = apiLoginGoogle(idToken)
                tokenStorage.saveToken(token)
                sessionToken = token
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                onError(e.message ?: "Google sign-in failed")
            }
        }
    }

    fun signup(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                apiSignup(email, password)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Signup failed")
            }
        }
    }

    /**
     * Logs out. The local data goes with the session, so the changes not pushed yet get a few
     * seconds to reach the server first.
     */
    fun logout() {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            withTimeoutOrNull(LOGOUT_SYNC_TIMEOUT_MS) {
                try {
                    Sync.syncNow()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Offline: the pending changes are lost with the session.
                }
            }
            endSession()
        }
    }

    /** Forgets the session and deletes the local data, so that the next account starts empty. */
    private suspend fun endSession() {
        sessionToken = null
        tokenStorage.clearToken()
        clearSessionCookies()
        cancelBackgroundSync()
        Sync.clear()
        _authState.value = AuthState.Unauthenticated()
    }

    // Permanently deletes the account on the server, then clears the local
    // session exactly like logout. On failure the session is left intact so the
    // user can retry.
    fun deleteAccount(onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                apiDeleteAccount()
                endSession()
            } catch (e: Exception) {
                onError(e.message ?: "Account deletion failed")
            }
        }
    }
}
