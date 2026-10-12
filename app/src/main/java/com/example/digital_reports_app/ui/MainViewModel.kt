package com.example.digital_reports_app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_reports_app.data.local.SessionManager
import com.example.digital_reports_app.data.repository.AuthRepository
import com.example.digital_reports_app.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    val isDarkTheme: Flow<Boolean> = sessionManager.isDarkTheme

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId().first()
            _startDestination.value = if (userId != null) {
                Screen.HOME
            } else {
                Screen.LOGIN
            }
        }
    }
}
