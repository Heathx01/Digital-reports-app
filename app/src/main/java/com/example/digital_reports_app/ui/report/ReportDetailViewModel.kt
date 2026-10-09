package com.example.digital_reports_app.ui.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_reports_app.data.local.entity.ReportEntity
import com.example.digital_reports_app.data.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportDetailUiState(
    val report: ReportEntity? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ReportDetailViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val reportId: String = checkNotNull(savedStateHandle["reportId"])

    private val _uiState = MutableStateFlow(ReportDetailUiState())
    val uiState: StateFlow<ReportDetailUiState> = _uiState.asStateFlow()

    init {
        loadReport()
    }

    private fun loadReport() {
        viewModelScope.launch {
            reportRepository.getReportById(reportId).collect { report ->
                _uiState.update { it.copy(report = report, isLoading = false) }
            }
        }
    }

    fun updateStatus(newStatus: String) {
        viewModelScope.launch {
            reportRepository.updateReportStatus(reportId, newStatus)
        }
    }

    fun deleteReport() {
        viewModelScope.launch {
            reportRepository.deleteReport(reportId)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }
}
