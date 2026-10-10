package com.example.digital_reports_app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_reports_app.data.local.dao.UserDao
import com.example.digital_reports_app.data.local.entity.UserEntity
import com.example.digital_reports_app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val currentUser: UserEntity? = null,
    val currentPasswordInput: String = "",
    val newPasswordInput: String = "",
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId().first()
            if (userId != null) {
                val user = authRepository.getUserById(userId)
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    fun onCurrentPasswordChanged(input: String) {
        _uiState.update { it.copy(currentPasswordInput = input, errorMessage = null, successMessage = null) }
    }

    fun onNewPasswordChanged(input: String) {
        _uiState.update { it.copy(newPasswordInput = input, errorMessage = null, successMessage = null) }
    }

    fun changePassword() {
        val user = _uiState.value.currentUser ?: return
        val currentInput = _uiState.value.currentPasswordInput
        val newInput = _uiState.value.newPasswordInput

        if (currentInput.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa tu contraseña actual.") }
            return
        }

        if (currentInput != user.passwordHash) {
            _uiState.update { it.copy(errorMessage = "La contraseña actual no es correcta.") }
            return
        }

        if (newInput.length < 6) {
            _uiState.update { it.copy(errorMessage = "La nueva contraseña debe tener al menos 6 caracteres.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val updatedUser = user.copy(passwordHash = newInput)
            userDao.insertUsers(listOf(updatedUser))

            _uiState.update {
                it.copy(
                    currentUser = updatedUser,
                    currentPasswordInput = "",
                    newPasswordInput = "",
                    isLoading = false,
                    successMessage = "¡Contraseña actualizada con éxito!"
                )
            }
        }
    }
}
