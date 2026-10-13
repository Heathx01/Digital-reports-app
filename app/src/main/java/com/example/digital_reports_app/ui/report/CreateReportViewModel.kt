package com.example.digital_reports_app.ui.report

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
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
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

data class CreateReportUiState(
    val title: String = "",
    val category: String = "Inspección",
    val description: String = "",
    val isLoading: Boolean = false,
    val isAnalyzingDocument: Boolean = false,
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
            // Admin role check
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
                        description = entity.description,
                        fields = jsonToFields(entity.fieldsJson).toMutableList()
                    )
                }
                _uiState.update { it.copy(savedTemplates = converted) }
            }
        }
    }

    fun analyzeAndProcessScannedPhoto(
        bitmap: Bitmap,
        onAnalyzed: (title: String, category: String, description: String, fields: List<CustomFormField>) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingDocument = true) }
            val nextIndex = _uiState.value.savedTemplates.size + 1
            val result = DocumentTextAnalyzer.analyzeScannedBitmapWithMlKit(bitmap, nextIndex)

            _uiState.update { it.copy(isAnalyzingDocument = false) }
            onAnalyzed(result.templateTitle, result.category, result.description, result.extractedFields)
        }
    }

    fun analyzeAndProcessPdfUri(
        context: Context,
        pdfUri: Uri,
        onAnalyzed: (title: String, category: String, description: String, fields: List<CustomFormField>) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingDocument = true) }
            val nextIndex = _uiState.value.savedTemplates.size + 1
            val result = DocumentTextAnalyzer.analyzePdfUri(context, pdfUri, nextIndex)

            _uiState.update { it.copy(isAnalyzingDocument = false) }
            onAnalyzed(result.templateTitle, result.category, result.description, result.extractedFields)
        }
    }

    fun saveTemplateToDb(
        title: String,
        category: String,
        description: String,
        fields: List<CustomFormField>,
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
            fieldsJson = fieldsToJson(fields),
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

    private fun fieldsToJson(fields: List<CustomFormField>): String {
        val jsonArray = JSONArray()
        fields.forEach { f ->
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("label", f.label)
            obj.put("type", f.type.name)
            obj.put("value", f.value)
            val optionsArray = JSONArray()
            f.options.forEach { optionsArray.put(it) }
            obj.put("options", optionsArray)
            obj.put("positionX", f.positionX.toDouble())
            obj.put("positionY", f.positionY.toDouble())
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun jsonToFields(json: String): List<CustomFormField> {
        if (json.isBlank()) return DocumentTextAnalyzer.createDefaultFields()
        return try {
            val list = mutableListOf<CustomFormField>()
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "f_$i")
                val label = obj.optString("label", "Campo")
                val typeName = obj.optString("type", "TEXT")
                val type = try { FormFieldType.valueOf(typeName) } catch (_: Exception) { FormFieldType.TEXT }
                val value = obj.optString("value", "")
                val optionsList = mutableListOf<String>()
                val optionsArr = obj.optJSONArray("options")
                if (optionsArr != null) {
                    for (j in 0 until optionsArr.length()) {
                        optionsList.add(optionsArr.getString(j))
                    }
                }
                val posX = obj.optDouble("positionX", 180.0).toFloat()
                val posY = obj.optDouble("positionY", 600.0).toFloat()

                list.add(CustomFormField(id, label, type, value, optionsList, posX, posY))
            }
            list.ifEmpty { DocumentTextAnalyzer.createDefaultFields() }
        } catch (_: Exception) {
            try {
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
}
