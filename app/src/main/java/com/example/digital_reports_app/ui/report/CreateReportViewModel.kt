package com.example.digital_reports_app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_reports_app.data.local.SessionManager
import com.example.digital_reports_app.data.local.entity.TemplateEntity
import com.example.digital_reports_app.data.local.entity.UserEntity
import com.example.digital_reports_app.data.repository.AuthRepository
import com.example.digital_reports_app.data.repository.ReportRepository
import com.example.digital_reports_app.data.repository.TemplateRepository
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
    val savedTemplates: List<ReportTemplateItem> = emptyList(),
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val isAdmin: Boolean = false
)

@HiltViewModel
class CreateReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val templateRepository: TemplateRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateReportUiState())
    val uiState: StateFlow<CreateReportUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUserRole()
        loadTemplatesFromDb()
    }

    private fun loadCurrentUserRole() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId().first()
            val user = if (userId != null) authRepository.getUserById(userId) else null
            _uiState.update { it.copy(isAdmin = user?.role == "ADMIN") }
        }
    }

    private fun loadTemplatesFromDb() {
        viewModelScope.launch {
            templateRepository.getAllTemplates().collect { dbTemplates ->
                val converted = dbTemplates.map { entity ->
                    ReportTemplateItem(
                        id = entity.templateId,
                        title = entity.title,
                        category = entity.category,
                        description = entity.description
                    )
                }
                _uiState.update { it.copy(savedTemplates = converted) }
            }
        }
    }

    fun saveTemplateToDb(
        title: String,
        category: String,
        description: String,
        customId: String? = null
    ) {
        if (!_uiState.value.isAdmin) {
            _uiState.update { it.copy(errorMessage = "Solo los usuarios administradores pueden guardar o editar plantillas.") }
            return
        }

        val templateId = customId ?: "tpl_${System.currentTimeMillis()}"
        val entity = TemplateEntity(
            templateId = templateId,
            title = title.trim(),
            category = category.trim(),
            description = description.trim(),
            fieldsJson = "[]",
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            templateRepository.insertTemplate(entity)
        }
    }

    fun deleteTemplateFromDb(templateId: String) {
        if (!_uiState.value.isAdmin) {
            _uiState.update { it.copy(errorMessage = "Solo los usuarios administradores pueden eliminar plantillas.") }
            return
        }
        viewModelScope.launch {
            templateRepository.deleteTemplate(templateId)
        }
    }

    fun deleteTemplatesByIdsFromDb(templateIds: List<String>) {
        if (!_uiState.value.isAdmin) {
            _uiState.update { it.copy(errorMessage = "Solo los usuarios administradores pueden eliminar plantillas.") }
            return
        }
        viewModelScope.launch {
            templateRepository.deleteTemplatesByIds(templateIds)
        }
    }

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
