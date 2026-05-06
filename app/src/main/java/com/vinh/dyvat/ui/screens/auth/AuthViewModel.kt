package com.vinh.dyvat.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vinh.dyvat.data.model.Result
import com.vinh.dyvat.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val authState: AuthState = AuthState.Unknown,
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

sealed class AuthState {
    data object Unknown : AuthState()
    data object LoggedIn : AuthState()
    data object NotLoggedIn : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val tag = "AuthViewModel"

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun checkAuthState() {
        viewModelScope.launch {
            Log.d(tag, "checkAuthState: started")
            authRepository.getCurrentSession().collect { result ->
                _uiState.value = when (result) {
                    is Result.Loading -> {
                        Log.d(tag, "checkAuthState: loading")
                        _uiState.value.copy(isLoading = true, errorMessage = null)
                    }
                    is Result.Success -> {
                        if (result.data != null) {
                            Log.d(tag, "checkAuthState: logged in user=${result.data.id}")
                            _uiState.value.copy(isLoading = false, authState = AuthState.LoggedIn)
                        } else {
                            Log.d(tag, "checkAuthState: no session")
                            _uiState.value.copy(isLoading = false, authState = AuthState.NotLoggedIn)
                        }
                    }
                    is Result.Error -> {
                        Log.e(tag, "checkAuthState: error=${result.message}", result.exception)
                        _uiState.value.copy(
                            isLoading = false,
                            authState = AuthState.NotLoggedIn,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String, rawNonce: String) {
        viewModelScope.launch {
            Log.d(tag, "signInWithGoogle: started")
            authRepository.signInWithGoogle(idToken, rawNonce).collect { result ->
                _uiState.value = when (result) {
                    is Result.Loading -> {
                        Log.d(tag, "signInWithGoogle: loading")
                        _uiState.value.copy(isLoading = true, errorMessage = null)
                    }
                    is Result.Success -> {
                        Log.d(tag, "signInWithGoogle: success user=${result.data.id}")
                        _uiState.value.copy(
                            isLoading = false,
                            authState = AuthState.LoggedIn,
                            errorMessage = null
                        )
                    }
                    is Result.Error -> {
                        Log.e(tag, "signInWithGoogle: error=${result.message}", result.exception)
                        _uiState.value.copy(
                            isLoading = false,
                            authState = AuthState.Error(result.message),
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            Log.d(tag, "signOut: started")
            when (val result = authRepository.signOut()) {
                is Result.Success -> {
                    Log.d(tag, "signOut: success")
                    _uiState.value = AuthUiState(authState = AuthState.NotLoggedIn)
                }
                is Result.Error -> {
                    Log.e(tag, "signOut: error=${result.message}", result.exception)
                    _uiState.value = _uiState.value.copy(errorMessage = result.message)
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
