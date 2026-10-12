package com.example.digital_reports_app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_reports_app.data.local.SessionManager
import com.example.digital_reports_app.data.local.entity.TemplateEntity
import com.example.digital_reports_app.data.local.entity.UserEntity
import com.example.digital_reports_app.data.repository.AuthRepository
import com.example.digital_reports_app.data.repository.ReportRepository
import com.example.digital_reports_app.data.repository.TemplateRepository
import com.example.digital_reports_app.util.CustomFormField
import com.example.digital_reports_app.util.DocumentTextAnalyzer
import com.example.digital_reports_app.util.FormFieldType
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
    val isSuccess: Boolean = false
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
        loadTemplatesFromDb()
    }

    private fun loadTemplatesFromDb() {
        viewModelScope.launch {
            val hasSeeded = sessionManager.hasSeededTemplates.first()

            templateRepository.getAllTemplates().collect { dbTemplates ->
                if (dbTemplates.isEmpty() && !hasSeeded) {
                    seedInitialTemplates()
                    sessionManager.setHasSeededTemplates(true)
                } else {
                    val converted = dbTemplates.map { entity ->
                        ReportTemplateItem(
                            id = entity.templateId,
                            title = entity.title,
                            category = entity.category,
                            description = entity.description,
                            fields = jsonToFields(entity.fieldsJson).toMutableList()
                        )
                    }
                    _uiState.update { it.copy(savedTemplates = converted) }
                }
            }
        }
    }

    private suspend fun seedInitialTemplates() {
        val initialList = listOf(
            ReportTemplateItem(
                id = "tpl_1",
                title = "Inspección Diaria de Planta",
                category = "Inspección",
                description = "Verificación general de infraestructura, equipos y condiciones ambientales.",
                fields = DocumentTextAnalyzer.createDefaultFields().toMutableList()
            ),
            ReportTemplateItem(
                id = "tpl_2",
                title = "Mantenimiento Preventivo de Maquinaria",
                category = "Mantenimiento",
                description = "Revisión de lubricación, presión y componentes mecánicos de planta.",
                fields = mutableListOf(
                    CustomFormField("m_1", "Código de Maquinaria", FormFieldType.TEXT, "MQ-104"),
                    CustomFormField("m_2", "Nivel de Aceite", FormFieldType.DROPDOWN, "Normal", listOf("Normal", "Bajo", "Crítico")),
                    CustomFormField("m_3", "Presión (PSI)", FormFieldType.NUMBER, "120"),
                    CustomFormField("m_4", "Observaciones de Mantenimiento", FormFieldType.TEXT, "Piezas en buen estado.")
                )
            ),
            ReportTemplateItem(
                id = "tpl_3",
                title = "Control de Seguridad e Higiene",
                category = "Incidencia",
                description = "Auditoría de equipos EPP, ventilación y protocolos de riesgo laboral.",
                fields = mutableListOf(
                    CustomFormField("s_1", "Zona de Trabajo", FormFieldType.TEXT, "Socavón / Nivel 2"),
                    CustomFormField("s_2", "Uso de EPP", FormFieldType.DROPDOWN, "Conforme", listOf("Conforme", "Incompleto", "No Conforme")),
                    CustomFormField("s_3", "Medición de Gas (PPM)", FormFieldType.NUMBER, "12"),
                    CustomFormField("s_4", "Detalles de Seguridad", FormFieldType.TEXT, "Sin incidencias graves.")
                )
            ),
            ReportTemplateItem(
                id = "tpl_4",
                title = "Auditoría de Almacén y Embalaje",
                category = "General",
                description = "Revisión de inventarios, cajas y estado de carga de materiales.",
                fields = mutableListOf(
                    CustomFormField("a_1", "Sector de Almacén", FormFieldType.TEXT, "Almacén B"),
                    CustomFormField("a_2", "Estado de Empaque", FormFieldType.DROPDOWN, "Excelente", listOf("Excelente", "Dañado", "Incompleto")),
                    CustomFormField("a_3", "Cantidad de Cajas", FormFieldType.NUMBER, "450"),
                    CustomFormField("a_4", "Notas de Carga", FormFieldType.TEXT, "Carga lista para despacho.")
                )
            )
        )

        initialList.forEach { tpl ->
            saveTemplateToDb(tpl.title, tpl.category, tpl.description, tpl.fields, tpl.id)
        }
    }

    fun saveTemplateToDb(
        title: String,
        category: String,
        description: String,
        fields: List<CustomFormField>,
        customId: String? = null
    ) {
        val templateId = customId ?: "tpl_${System.currentTimeMillis()}"
        val entity = TemplateEntity(
            templateId = templateId,
            title = title.trim(),
            category = category.trim(),
            description = description.trim(),
            fieldsJson = fieldsToJson(fields),
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            templateRepository.insertTemplate(entity)
        }
    }

    fun deleteTemplateFromDb(templateId: String) {
        viewModelScope.launch {
            templateRepository.deleteTemplate(templateId)
        }
    }

    fun deleteTemplatesByIdsFromDb(templateIds: List<String>) {
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

    private fun fieldsToJson(fields: List<CustomFormField>): String {
        return fields.joinToString("|||") { f ->
            "${f.id}:::${f.label}:::${f.type.name}:::${f.value}:::${f.options.joinToString(",")}"
        }
    }

    private fun jsonToFields(json: String): List<CustomFormField> {
        if (json.isBlank()) return DocumentTextAnalyzer.createDefaultFields()
        return try {
            json.split("|||").mapNotNull { line ->
                val parts = line.split(":::")
                if (parts.size >= 4) {
                    val typeName = parts[2]
                    val type = try { FormFieldType.valueOf(typeName) } catch (_: Exception) { FormFieldType.TEXT }
                    val options = if (parts.size >= 5 && parts[4].isNotBlank()) parts[4].split(",") else emptyList()
                    CustomFormField(parts[0], parts[1], type, parts[3], options)
                } else null
            }.ifEmpty { DocumentTextAnalyzer.createDefaultFields() }
        } catch (_: Exception) {
            DocumentTextAnalyzer.createDefaultFields()
        }
    }
}
