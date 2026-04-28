package com.vinh.dyvat.ui.screens.auth

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

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun checkAuthState() {
        viewModelScope.launch {
            authRepository.getCurrentSession().collect { result ->
                _uiState.value = when (result) {
                    is Result.Loading -> _uiState.value.copy(isLoading = true, errorMessage = null)
                    is Result.Success -> {
                        if (result.data != null) {
                            _uiState.value.copy(isLoading = false, authState = AuthState.LoggedIn)
                        } else {
                            _uiState.value.copy(isLoading = false, authState = AuthState.NotLoggedIn)
                        }
                    }
                    is Result.Error -> _uiState.value.copy(
                        isLoading = false,
                        authState = AuthState.NotLoggedIn
                    )
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String, rawNonce: String) {
        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken, rawNonce).collect { result ->
                _uiState.value = when (result) {
                    is Result.Loading -> _uiState.value.copy(isLoading = true, errorMessage = null)
                    is Result.Success -> _uiState.value.copy(
                        isLoading = false,
                        authState = AuthState.LoggedIn,
                        errorMessage = null
                    )
                    is Result.Error -> _uiState.value.copy(
                        isLoading = false,
                        authState = AuthState.Error(result.message),
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.value = AuthUiState(authState = AuthState.NotLoggedIn)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
