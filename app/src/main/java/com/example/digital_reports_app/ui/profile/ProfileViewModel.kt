package com.example.digital_reports_app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_reports_app.data.local.SessionManager
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
    val isDarkTheme: Boolean = false,
    val themeMode: String = "SYSTEM", // "DARK", "LIGHT", "SYSTEM"
    val selectedLanguage: String = "Español",
    val isLoading: Boolean = false,
    val showThemeModeDialog: Boolean = false,
    val showEditProfileDialog: Boolean = false,
    val showLanguageDialog: Boolean = false,
    val showUpdateVersionDialog: Boolean = false,
    val showLogoutConfirmDialog: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val isLoggedOut: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId().first()
            if (userId != null) {
                val user = authRepository.getUserById(userId)
                _uiState.update { it.copy(currentUser = user) }
            }

            sessionManager.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode, isDarkTheme = (mode == "DARK")) }
            }
        }

        viewModelScope.launch {
            sessionManager.appLanguage.collect { lang ->
                _uiState.update { it.copy(selectedLanguage = lang) }
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            sessionManager.setThemeMode(mode)
            _uiState.update { it.copy(themeMode = mode, showThemeModeDialog = false) }
        }
    }

    fun toggleThemeModeDialog(show: Boolean) {
        _uiState.update { it.copy(showThemeModeDialog = show) }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            sessionManager.setAppLanguage(language)
        }
    }

    fun toggleEditProfileDialog(show: Boolean) {
        _uiState.update { it.copy(showEditProfileDialog = show, errorMessage = null, successMessage = null) }
    }

    fun toggleLanguageDialog(show: Boolean) {
        _uiState.update { it.copy(showLanguageDialog = show) }
    }

    fun toggleUpdateVersionDialog(show: Boolean) {
        _uiState.update { it.copy(showUpdateVersionDialog = show) }
    }

    fun toggleLogoutConfirmDialog(show: Boolean) {
        _uiState.update { it.copy(showLogoutConfirmDialog = show) }
    }

    fun updateProfileAndCredentials(
        newName: String,
        newCode: String,
        currentPasswordInput: String,
        newPasswordInput: String
    ) {
        val user = _uiState.value.currentUser ?: return
        if (newName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El nombre no puede estar vacío.") }
            return
        }

        if (newCode.length < 6) {
            _uiState.update { it.copy(errorMessage = "El código de usuario debe tener al menos 6 caracteres.") }
            return
        }

        var updatedPassword = user.passwordHash

        // If user is attempting to change password
        if (newPasswordInput.isNotBlank() || currentPasswordInput.isNotBlank()) {
            if (currentPasswordInput.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Ingresa tu contraseña actual para autorizar el cambio.") }
                return
            }

            if (currentPasswordInput != user.passwordHash) {
                _uiState.update { it.copy(errorMessage = "La contraseña actual es incorrecta.") }
                return
            }

            if (newPasswordInput.length < 8 || !newPasswordInput.any { it.isUpperCase() } || !newPasswordInput.any { it.isDigit() } || !newPasswordInput.any { !it.isLetterOrDigit() }) {
                _uiState.update { it.copy(errorMessage = "La nueva contraseña debe tener al menos 8 caracteres, una mayúscula, un número y un carácter especial.") }
                return
            }

            updatedPassword = newPasswordInput
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val updatedUser = user.copy(
                name = newName.trim(),
                userCode = newCode.trim(),
                passwordHash = updatedPassword
            )
            userDao.insertUsers(listOf(updatedUser))
            sessionManager.saveSession(updatedUser.userId, updatedUser.name, updatedUser.userCode)

            _uiState.update {
                it.copy(
                    currentUser = updatedUser,
                    isLoading = false,
                    showEditProfileDialog = false,
                    successMessage = "Perfil y credenciales actualizados con éxito"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(isLoggedOut = true, showLogoutConfirmDialog = false) }
        }
    }
}
