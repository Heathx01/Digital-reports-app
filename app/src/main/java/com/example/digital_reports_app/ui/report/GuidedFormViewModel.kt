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
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val initial = mutableMapOf<String, String>()
        form.pages.forEach { page ->
            page.headerFields.filter { it.isDate }.forEach {
                initial[FormKeys.header(page.id, it.id)] = today
            }
        }
        _uiState.update { it.copy(form = form, stepIndex = 0, answers = initial, errorMessage = null) }
    }

    fun backToList() {
        _uiState.update { it.copy(form = null, stepIndex = 0, answers = emptyMap(), errorMessage = null) }
    }

    fun setAnswer(key: String, value: String) {
        _uiState.update { it.copy(answers = it.answers + (key to value), errorMessage = null) }
    }

    fun toggleAnswer(key: String, value: String) {
        val current = _uiState.value.answers[key]
        setAnswer(key, if (current == value) "" else value)
    }

    fun next() {
        _uiState.update { it.copy(stepIndex = (it.stepIndex + 1).coerceAtMost((it.steps.size - 1).coerceAtLeast(0))) }
    }

    fun previous() {
        if (_uiState.value.stepIndex == 0) backToList()
        else _uiState.update { it.copy(stepIndex = it.stepIndex - 1) }
    }

    fun save() {
        val state = _uiState.value
        val form = state.form ?: return
        val answers = state.answers

        val answeredItems = form.pages.flatMap { p -> p.sections.flatMap { it.items } }
            .count { answers[FormKeys.answer(it.id)].orEmpty().isNotEmpty() }
        if (answeredItems == 0) {
            _uiState.update { it.copy(errorMessage = "Responde al menos un punto del formato antes de guardar.") }
            return
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
            val description = "Formato ${form.code}: $answeredItems de $total puntos respondidos" +
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
}
