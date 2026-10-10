package com.example.digital_reports_app.ui.home

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_reports_app.data.local.entity.UserEntity
import com.example.digital_reports_app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timeFormatted: String,
    val isRead: Boolean = false
)

data class HomeUiState(
    val currentUser: UserEntity? = null,
    val isLoading: Boolean = true,
    val isOnline: Boolean = true,
    val showNotificationsDialog: Boolean = false,
    val showTemplatesDialog: Boolean = false,
    val notifications: List<NotificationItem> = listOf(
        NotificationItem("1", "Sincronización Automática", "Todos los expedientes están respaldados localmente.", "Hace 5 min"),
        NotificationItem("2", "Estado de Red", "Conexión de telemetría activa.", "Hace 15 min")
    ),
    val isLoggedOut: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    init {
        loadUserSession()
        observeConnectivity()
    }

    private fun loadUserSession() {
        viewModelScope.launch {
            authRepository.getCurrentUserId().collect { userId ->
                if (userId != null) {
                    val user = authRepository.getUserById(userId)
                    _uiState.update { it.copy(currentUser = user, isLoading = false) }
                } else {
                    _uiState.update { it.copy(currentUser = null, isLoading = false, isLoggedOut = true) }
                }
            }
        }
    }

    private fun observeConnectivity() {
        connectivityManager?.let { cm ->
            val activeNetwork = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(activeNetwork)
            val initialOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            _uiState.update { it.copy(isOnline = initialOnline) }

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            try {
                cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        _uiState.update { it.copy(isOnline = true) }
                    }

                    override fun onLost(network: Network) {
                        _uiState.update { it.copy(isOnline = false) }
                    }
                })
            } catch (_: Exception) { }
        }
    }

    fun toggleNotificationsDialog(show: Boolean) {
        _uiState.update { it.copy(showNotificationsDialog = show) }
    }

    fun toggleTemplatesDialog(show: Boolean) {
        _uiState.update { it.copy(showTemplatesDialog = show) }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(isLoggedOut = true) }
        }
    }
}
