package com.example.digital_reports_app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_reports_app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val userCode: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val userCodeError: String? = null,
    val passwordError: String? = null,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUserCodeChanged(userCode: String) {
        val trimmed = userCode.trim()
        val error = when {
            trimmed.isNotEmpty() && trimmed.length < 6 -> "El código de trabajador debe tener al menos 6 caracteres."
            else -> null
        }
        _uiState.update { 
            it.copy(
                userCode = userCode,
                userCodeError = error,
                errorMessage = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        val error = when {
            password.isNotEmpty() && password.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
            password.isNotEmpty() && !password.any { it.isUpperCase() } -> "Debe incluir al menos una letra mayúscula."
            password.isNotEmpty() && !password.any { it.isDigit() } -> "Debe incluir al menos un número."
            password.isNotEmpty() && !password.any { !it.isLetterOrDigit() } -> "Debe incluir al menos un carácter especial (#, $, !, etc.)."
            else -> null
        }
        _uiState.update { 
            it.copy(
                password = password,
                passwordError = error,
                errorMessage = null
            )
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null, userCodeError = null, passwordError = null) }
    }

    fun login() {
        val userCode = _uiState.value.userCode.trim()
        val password = _uiState.value.password

        var isValid = true

        val userCodeErr = when {
            userCode.isBlank() -> "Por favor ingresa tu código de trabajador."
            userCode.length < 6 -> "El código de trabajador debe tener al menos 6 caracteres."
            else -> null
        }
        if (userCodeErr != null) {
            _uiState.update { it.copy(userCodeError = userCodeErr) }
            isValid = false
        }

        val passwordErr = when {
            password.isBlank() -> "Por favor ingresa tu contraseña."
            password.length < 8 -> "La contraseña debe tener al menos 8 caracteres."
            !password.any { it.isUpperCase() } -> "La contraseña debe incluir al menos una letra mayúscula."
            !password.any { it.isDigit() } -> "La contraseña debe incluir al menos un número."
            !password.any { !it.isLetterOrDigit() } -> "La contraseña debe incluir al menos un carácter especial."
            else -> null
        }
        if (passwordErr != null) {
            _uiState.update { it.copy(passwordError = passwordErr) }
            isValid = false
        }

        if (!isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.login(userCode, password)
            result.onSuccess {
                _uiState.update { state ->
                    state.copy(isLoading = false, isLoggedIn = true)
                }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al iniciar sesión. Intenta nuevamente."
                    )
                }
            }
        }
    }
}
