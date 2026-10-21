package com.example.digital_reports_app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_reports_app.data.local.dao.UserDao
import com.example.digital_reports_app.data.local.entity.UserEntity
import com.example.digital_reports_app.util.SecurityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserManagementUiState(
    val users: List<UserEntity> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateUserDialog: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            userDao.getAssignedUsers().collect { userList ->
                _uiState.update { it.copy(users = userList, isLoading = false) }
            }
        }
    }

    fun toggleCreateUserDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateUserDialog = show, errorMessage = null, successMessage = null) }
    }

    fun createUser(
        name: String,
        userCode: String,
        passwordInput: String,
        role: String,
        shift: String
    ) {
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa el nombre del trabajador.") }
            return
        }

        if (userCode.trim().length < 6) {
            _uiState.update { it.copy(errorMessage = "El código debe tener al menos 6 dígitos.") }
            return
        }

        if (passwordInput.length < 8) {
            _uiState.update { it.copy(errorMessage = "La contraseña debe tener al menos 8 caracteres.") }
            return
        }

        viewModelScope.launch {
            val existing = userDao.getUserByCode(userCode.trim())
            if (existing != null) {
                _uiState.update { it.copy(errorMessage = "Ya existe un usuario con el código '$userCode'.") }
                return@launch
            }

            val newUser = UserEntity(
                userId = "usr_${userCode.trim()}",
                userCode = userCode.trim(),
                name = name.trim(),
                passwordHash = SecurityUtils.hashPassword(passwordInput),
                role = role,
                shift = shift,
                isAssignedToDevice = true
            )

            userDao.insertUsers(listOf(newUser))
            _uiState.update {
                it.copy(
                    showCreateUserDialog = false,
                    successMessage = "Usuario '${newUser.name}' registrado exitosamente."
                )
            }
        }
    }
}
