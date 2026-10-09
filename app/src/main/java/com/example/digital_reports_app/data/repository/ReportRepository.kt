package com.example.digital_reports_app.data.repository

import com.example.digital_reports_app.data.local.entity.ReportEntity
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    fun getReports(authorCode: String, isAdmin: Boolean): Flow<List<ReportEntity>>
    fun getReportById(reportId: String): Flow<ReportEntity?>
    suspend fun createReport(
        title: String,
        category: String,
        description: String,
        authorCode: String,
        authorName: String
    ): Result<Unit>
    suspend fun updateReportStatus(reportId: String, status: String): Result<Unit>
    suspend fun deleteReport(reportId: String): Result<Unit>
}
