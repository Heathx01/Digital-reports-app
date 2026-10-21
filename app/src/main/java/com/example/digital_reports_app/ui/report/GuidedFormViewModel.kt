package com.example.digital_reports_app.ui.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_reports_app.data.repository.AuthRepository
import com.example.digital_reports_app.data.repository.ReportRepository
import com.example.digital_reports_app.domain.model.FormCatalog
import com.example.digital_reports_app.domain.model.FormKeys
import com.example.digital_reports_app.domain.model.FormPage
import com.example.digital_reports_app.domain.model.FormSection
import com.example.digital_reports_app.domain.model.RowKind
import com.example.digital_reports_app.domain.model.StructuredForm
import com.example.digital_reports_app.domain.model.answersToJson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/** Pasos del asistente: cada sección del formato es un paso. */
sealed class GuidedStep {
    abstract val page: FormPage
    abstract val title: String

    data class Header(override val page: FormPage) : GuidedStep() {
        override val title = "Datos generales"
    }

    data class Checklist(override val page: FormPage, val section: FormSection) : GuidedStep() {
        override val title = section.title
    }

    data class Closing(override val page: FormPage) : GuidedStep() {
        override val title = "Comentarios y firmas"
    }
}

fun stepsOf(form: StructuredForm): List<GuidedStep> = form.pages.flatMap { page ->
    buildList {
        add(GuidedStep.Header(page))
        page.sections.forEach { add(GuidedStep.Checklist(page, it)) }
        add(GuidedStep.Closing(page))
    }
}

data class GuidedFormUiState(
    val forms: List<StructuredForm> = FormCatalog.all,
    val form: StructuredForm? = null,
    val stepIndex: Int = 0,
    val answers: Map<String, String> = emptyMap(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedReportId: String? = null
) {
    val steps: List<GuidedStep> get() = form?.let { stepsOf(it) } ?: emptyList()
}

@HiltViewModel
class GuidedFormViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuidedFormUiState())
    val uiState: StateFlow<GuidedFormUiState> = _uiState.asStateFlow()

    init {
        val initialFormId: String? = savedStateHandle["formId"]
        if (!initialFormId.isNullOrBlank()) {
            selectForm(initialFormId)
        }
    }

    fun selectForm(formId: String) {
        val form = FormCatalog.byId(formId) ?: return
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now.time)
        val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now.time)
        val shift = if (hour in 6..17) "DIURNO" else "NOCTURNO"

        val initial = mutableMapOf<String, String>()
        form.pages.forEach { page ->
            page.headerFields.forEach { field ->
                val key = FormKeys.header(page.id, field.id)
                when {
                    field.isDate || field.id.contains("fecha", ignoreCase = true) -> initial[key] = today
                    field.id.contains("hora", ignoreCase = true) -> initial[key] = timeNow
                    field.id.contains("turno", ignoreCase = true) -> initial[key] = shift
                }
            }
        }
        _uiState.update { it.copy(form = form, stepIndex = 0, answers = initial, errorMessage = null) }
    }

    fun backToList() {
        _uiState.update { it.copy(form = null, stepIndex = 0, answers = emptyMap(), errorMessage = null) }
    }

    fun setAnswer(key: String, value: String) {
        val currentAnswers = _uiState.value.answers.toMutableMap()
        currentAnswers[key] = value

        // Auto-evaluate CUMPLE / NO CUMPLE based on measurement threshold
        val form = _uiState.value.form
        if (form != null && key.endsWith(".val")) {
            val itemId = key.removeSuffix(".val")
            val item = form.pages.flatMap { it.sections }.flatMap { it.items }.firstOrNull { it.id == itemId }
            if (item != null) {
                val compliance = item.checkNumericCompliance(value)
                if (compliance == true) {
                    currentAnswers[FormKeys.answer(itemId)] = "CUMPLE"
                } else if (compliance == false) {
                    currentAnswers[FormKeys.answer(itemId)] = "NO CUMPLE"
                }
            }
        }

        _uiState.update { it.copy(answers = currentAnswers, errorMessage = null) }
    }

    fun toggleAnswer(key: String, value: String) {
        val current = _uiState.value.answers[key]
        setAnswer(key, if (current == value) "" else value)
    }

    fun next() {
        val state = _uiState.value
        val steps = state.steps
        if (steps.isEmpty()) return

        val currentStep = steps[state.stepIndex.coerceIn(0, steps.lastIndex)]
        val validationError = validateStep(currentStep, state.answers)

        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        _uiState.update {
            it.copy(
                stepIndex = (it.stepIndex + 1).coerceAtMost((it.steps.size - 1).coerceAtLeast(0)),
                errorMessage = null
            )
        }
    }

    fun previous() {
        if (_uiState.value.stepIndex == 0) backToList()
        else _uiState.update { it.copy(stepIndex = it.stepIndex - 1, errorMessage = null) }
    }

    fun save() {
        val state = _uiState.value
        val form = state.form ?: return
        val steps = state.steps
        val answers = state.answers

        if (steps.isNotEmpty()) {
            val lastStep = steps.last()
            val validationError = validateStep(lastStep, answers)
            if (validationError != null) {
                _uiState.update { it.copy(errorMessage = validationError) }
                return
            }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val userId = authRepository.getCurrentUserId().first()
            val user = if (userId != null) authRepository.getUserById(userId) else null

            val place = form.pages.firstNotNullOfOrNull { page ->
                page.headerFields
                    .firstOrNull { it.id == "nombre_finca" || it.id == "area" }
                    ?.let { answers[FormKeys.header(page.id, it.id)] }
                    ?.takeIf { it.isNotBlank() }
            }
            val title = if (place != null) "${form.name} – $place" else form.name

            val total = form.pages.sumOf { p -> p.sections.sumOf { it.items.size } }
            val negatives = form.pages.flatMap { p -> p.sections.flatMap { s -> s.items.map { s to it } } }
                .count { (s, item) -> answers[FormKeys.answer(item.id)] == s.kind.negative }
            val description = "Formato ${form.code}: $total de $total puntos respondidos" +
                if (negatives > 0) ", $negatives con hallazgo (${"NO / NO CUMPLE"})." else "."

            val result = reportRepository.createStructuredReport(
                title = title,
                category = form.category,
                description = description,
                formTemplateId = form.id,
                formDataJson = answersToJson(answers),
                authorCode = user?.userCode ?: "215800",
                authorName = user?.name ?: "Administrador"
            )

            result.onSuccess { id ->
                _uiState.update { it.copy(isSaving = false, savedReportId = id) }
            }.onFailure { e ->
                _uiState.update { it.copy(isSaving = false, errorMessage = "No se pudo guardar: ${e.message}") }
            }
        }
    }

    private fun validateStep(step: GuidedStep, answers: Map<String, String>): String? {
        when (step) {
            is GuidedStep.Header -> {
                step.page.headerFields.forEach { f ->
                    val value = answers[FormKeys.header(step.page.id, f.id)].orEmpty().trim()
                    if (value.isBlank()) {
                        return "Por favor completa el campo '${f.label.replace(":", "")}' para continuar."
                    }
                }
            }
            is GuidedStep.Checklist -> {
                step.section.items.forEach { item ->
                    val ans = answers[FormKeys.answer(item.id)].orEmpty().trim()
                    if (ans.isBlank()) {
                        return "Por favor evalúa el punto: '${item.label}' (Selecciona ${step.section.kind.positive} o ${step.section.kind.negative})."
                    }
                    if (step.section.kind == RowKind.MEASURE && item.unit != null) {
                        val measureVal = answers[FormKeys.value(item.id)].orEmpty().trim()
                        if (measureVal.isBlank()) {
                            return "Por favor ingresa el valor de medición para '${item.label}'."
                        }
                    }

                    // Discrepancy Validation between Numeric Value and CUMPLE / NO CUMPLE selection
                    val valueText = answers[FormKeys.value(item.id)].orEmpty().trim()
                    val compliance = item.checkNumericCompliance(valueText)
                    if (compliance == false && ans.equals("CUMPLE", ignoreCase = true)) {
                        val thresholdMsg = item.formatThreshold()
                        return "⚠️ Discrepancia en '${item.label}': El valor de medición ($valueText) excede el límite permitido ($thresholdMsg). Debe marcarse como 'NO CUMPLE'."
                    }
                    if (compliance == true && ans.equals("NO CUMPLE", ignoreCase = true)) {
                        val thresholdMsg = item.formatThreshold()
                        return "⚠️ Discrepancia en '${item.label}': El valor de medición ($valueText) sí cumple con los parámetros exigidos ($thresholdMsg). Debe marcarse como 'CUMPLE'."
                    }
                }
            }
            is GuidedStep.Closing -> {
                step.page.signatureLabels.forEachIndexed { i, label ->
                    val nameVal = answers[FormKeys.sigName(step.page.id, i)].orEmpty().trim()
                    val strokesVal = answers[FormKeys.sigStrokes(step.page.id, i)].orEmpty().trim()
                    if (nameVal.isBlank()) {
                        return "Por favor ingresa el nombre para '$label'."
                    }
                    if (strokesVal.isBlank()) {
                        return "Por favor dibuja la firma para '$label'."
                    }
                }
            }
        }
        return null
    }
}
