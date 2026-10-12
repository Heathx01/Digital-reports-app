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
    val selectedLanguage: String = "Español",
    val currentPasswordInput: String = "",
    val newPasswordInput: String = "",
    val isLoading: Boolean = false,
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

            sessionManager.isDarkTheme.collect { isDark ->
                _uiState.update { it.copy(isDarkTheme = isDark) }
            }
        }

        viewModelScope.launch {
            sessionManager.appLanguage.collect { lang ->
                _uiState.update { it.copy(selectedLanguage = lang) }
            }
        }
    }

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            sessionManager.setDarkTheme(isDark)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            sessionManager.setAppLanguage(language)
        }
    }

    fun toggleEditProfileDialog(show: Boolean) {
        _uiState.update { it.copy(showEditProfileDialog = show) }
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

    fun updateUserProfile(newName: String, newCode: String) {
        val user = _uiState.value.currentUser ?: return
        if (newName.isBlank() || newCode.isBlank()) return

        viewModelScope.launch {
            val updatedUser = user.copy(
                name = newName.trim(),
                userCode = newCode.trim()
            )
            userDao.insertUsers(listOf(updatedUser))
            sessionManager.saveSession(updatedUser.userId, updatedUser.name, updatedUser.userCode)

            _uiState.update {
                it.copy(
                    currentUser = updatedUser,
                    showEditProfileDialog = false,
                    successMessage = "Perfil actualizado con éxito"
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
