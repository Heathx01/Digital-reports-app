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

/** Plantillas instaladas por defecto en cada dispositivo: se pueden editar, pero NO eliminar. */
val PREDEFINED_TEMPLATE_IDS: Set<String> = setOf("tpl_1", "tpl_2", "tpl_3", "tpl_4")

/**
 * SUBIR este número cada vez que cambies la definición de una plantilla predefinida
 * (predefinedTemplates). Al instalar el APK nuevo como actualización, las plantillas
 * predefinidas se reemplazan por la versión nueva en cada dispositivo.
 */
const val PREDEFINED_CATALOG_VERSION: Int = 1

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
                val existingIds = dbTemplates.map { it.templateId }.toSet()
                val installedVersion = sessionManager.installedCatalogVersion.first()
                val needsUpgrade = installedVersion < PREDEFINED_CATALOG_VERSION
                val missing = PREDEFINED_TEMPLATE_IDS.any { it !in existingIds }

                if (needsUpgrade || missing) {
                    // Primera instalación, actualización del APK o plantilla faltante.
                    installPredefinedTemplates(overwriteExisting = needsUpgrade, existingIds = existingIds)
                    sessionManager.setInstalledCatalogVersion(PREDEFINED_CATALOG_VERSION)
                    sessionManager.setHasSeededTemplates(true)
                } else {
                    val converted = dbTemplates.map { entity ->
                        ReportTemplateItem(
                            id = entity.templateId,
                            title = entity.title,
                            category = entity.category,
                            description = entity.description,
                            fields = jsonToFields(entity.fieldsJson).toMutableList(),
                            isPredefined = entity.templateId in PREDEFINED_TEMPLATE_IDS
                        )
                    }
                    _uiState.update { it.copy(savedTemplates = converted) }
                }
            }
        }
    }

    /**
     * Instala las plantillas predefinidas del código. Con [overwriteExisting] (cuando sube
     * PREDEFINED_CATALOG_VERSION) reemplaza también las ya instaladas con la definición nueva;
     * si no, solo agrega las que falten. Las plantillas creadas por el admin no se tocan.
     */
    private suspend fun installPredefinedTemplates(overwriteExisting: Boolean, existingIds: Set<String>) {
        predefinedTemplates()
            .filter { overwriteExisting || it.id !in existingIds }
            .forEach { tpl ->
                templateRepository.insertTemplate(
                    TemplateEntity(
                        templateId = tpl.id,
                        title = tpl.title,
                        category = tpl.category,
                        description = tpl.description,
                        fieldsJson = fieldsToJson(tpl.fields),
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
    }

    private fun predefinedTemplates(): List<ReportTemplateItem> = listOf(
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
        if (templateId in PREDEFINED_TEMPLATE_IDS) return
        viewModelScope.launch {
            templateRepository.deleteTemplate(templateId)
        }
    }

    fun deleteTemplatesByIdsFromDb(templateIds: List<String>) {
        val deletable = templateIds.filterNot { it in PREDEFINED_TEMPLATE_IDS }
        if (deletable.isEmpty()) return
        viewModelScope.launch {
            templateRepository.deleteTemplatesByIds(deletable)
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
