package com.xavierclavel.bankable.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xavierclavel.bankable.api.apiLogin
import com.xavierclavel.bankable.api.apiLoginGoogle
import com.xavierclavel.bankable.api.apiSignup
import com.xavierclavel.bankable.api.sessionToken
import com.xavierclavel.bankable.api.unauthorizedFlow
import com.xavierclavel.bankable.storage.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    data class Unauthenticated(val error: String? = null) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val tokenStorage = TokenStorage(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState

    init {
        checkAuth()
        viewModelScope.launch {
            unauthorizedFlow.collect { logout() }
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

    fun loginWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val token = apiLoginGoogle(idToken)
                tokenStorage.saveToken(token)
                sessionToken = token
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Unauthenticated(e.message ?: "Google sign-in failed")
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

    fun logout() {
        sessionToken = null
        tokenStorage.clearToken()
        _authState.value = AuthState.Unauthenticated()
    }
}
