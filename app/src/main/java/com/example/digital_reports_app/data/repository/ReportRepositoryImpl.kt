package com.example.digital_reports_app.data.repository

import com.example.digital_reports_app.data.local.dao.ReportDao
import com.example.digital_reports_app.data.local.entity.ReportEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val reportDao: ReportDao
) : ReportRepository {

    override fun getReports(authorCode: String, isAdmin: Boolean): Flow<List<ReportEntity>> {
        return if (isAdmin) {
            reportDao.getAllReports()
        } else {
            reportDao.getReportsByAuthor(authorCode)
        }
    }

    override fun getReportById(reportId: String): Flow<ReportEntity?> {
        return reportDao.getReportById(reportId)
    }

    override suspend fun createReport(
        title: String,
        category: String,
        description: String,
        authorCode: String,
        authorName: String
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
            val dateFormatted = dateFormat.format(Date(now))
            val reportId = "REP-${UUID.randomUUID().toString().take(8).uppercase()}"

            val report = ReportEntity(
                reportId = reportId,
                title = title.trim(),
                category = category.trim(),
                description = description.trim(),
                status = "PENDIENTE",
                dateFormatted = dateFormatted,
                timestamp = now,
                authorCode = authorCode,
                authorName = authorName
            )

            reportDao.insertReport(report)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createStructuredReport(
        title: String,
        category: String,
        description: String,
        formTemplateId: String,
        formDataJson: String,
        authorCode: String,
        authorName: String
    ): Result<String> {
        return try {
            val now = System.currentTimeMillis()
            val dateFormatted = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault()).format(Date(now))
            val reportId = "REP-${UUID.randomUUID().toString().take(8).uppercase()}"
            reportDao.insertReport(
                ReportEntity(
                    reportId = reportId,
                    title = title.trim(),
                    category = category.trim(),
                    description = description.trim(),
                    status = "COMPLETADO",
                    dateFormatted = dateFormatted,
                    timestamp = now,
                    authorCode = authorCode,
                    authorName = authorName,
                    formTemplateId = formTemplateId,
                    formDataJson = formDataJson
                )
            )
            Result.success(reportId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateReportStatus(reportId: String, status: String): Result<Unit> {
        return try {
            reportDao.updateReportStatus(reportId, status)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteReport(reportId: String): Result<Unit> {
        return try {
            reportDao.deleteReport(reportId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
