package com.example.digital_reports_app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_reports_app.data.local.entity.UserEntity
import com.example.digital_reports_app.data.repository.AuthRepository
import com.example.digital_reports_app.data.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateReportUiState(
    val title: String = "",
    val category: String = "Inspección",
    val description: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class CreateReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateReportUiState())
    val uiState: StateFlow<CreateReportUiState> = _uiState.asStateFlow()

    val availableCategories = listOf("Inspección", "Mantenimiento", "Incidencia", "General")

    fun onTitleChanged(title: String) {
        _uiState.update { it.copy(title = title, errorMessage = null) }
    }

    fun onCategoryChanged(category: String) {
        _uiState.update { it.copy(category = category, errorMessage = null) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description, errorMessage = null) }
    }

    fun createReport() {
        val title = _uiState.value.title
        val category = _uiState.value.category
        val description = _uiState.value.description

        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor ingresa un título para el reporte.") }
            return
        }

        if (description.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor ingresa una descripción detallada.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val userId = authRepository.getCurrentUserId().first()
            var currentUser: UserEntity? = null
            if (userId != null) {
                currentUser = authRepository.getUserById(userId)
            }

            val authorCode = currentUser?.userCode ?: "215800"
            val authorName = currentUser?.name ?: "Administrador"

            val result = reportRepository.createReport(
                title = title,
                category = category,
                description = description,
                authorCode = authorCode,
                authorName = authorName
            )

            result.onSuccess {
                _uiState.update { state -> state.copy(isLoading = false, isSuccess = true) }
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Error al guardar el reporte. Intenta de nuevo."
                    )
                }
            }
        }
    }
}
