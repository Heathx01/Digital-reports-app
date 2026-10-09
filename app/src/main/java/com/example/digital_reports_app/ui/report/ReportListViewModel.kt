package com.example.digital_reports_app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digital_reports_app.data.local.entity.ReportEntity
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

data class ReportListUiState(
    val reports: List<ReportEntity> = emptyList(),
    val filteredReports: List<ReportEntity> = emptyList(),
    val selectedStatusFilter: String = "TODOS",
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val currentUser: UserEntity? = null
)

@HiltViewModel
class ReportListViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportListUiState())
    val uiState: StateFlow<ReportListUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val userId = authRepository.getCurrentUserId().first()
            var user: UserEntity? = null
            if (userId != null) {
                user = authRepository.getUserById(userId)
            }

            _uiState.update { it.copy(currentUser = user) }

            val authorCode = user?.userCode ?: "215800"
            val isAdmin = user?.role == "ADMIN"

            reportRepository.getReports(authorCode, isAdmin).collect { reportsList ->
                _uiState.update { state ->
                    val filtered = applyFilters(reportsList, state.selectedStatusFilter, state.searchQuery)
                    state.copy(
                        reports = reportsList,
                        filteredReports = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onStatusFilterSelected(status: String) {
        _uiState.update { state ->
            val filtered = applyFilters(state.reports, status, state.searchQuery)
            state.copy(selectedStatusFilter = status, filteredReports = filtered)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = applyFilters(state.reports, state.selectedStatusFilter, query)
            state.copy(searchQuery = query, filteredReports = filtered)
        }
    }

    private fun applyFilters(
        list: List<ReportEntity>,
        statusFilter: String,
        query: String
    ): List<ReportEntity> {
        return list.filter { report ->
            val matchesStatus = if (statusFilter == "TODOS") true else report.status == statusFilter
            val matchesQuery = query.isBlank() ||
                    report.title.contains(query, ignoreCase = true) ||
                    report.category.contains(query, ignoreCase = true) ||
                    report.description.contains(query, ignoreCase = true)
            matchesStatus && matchesQuery
        }
    }
}
