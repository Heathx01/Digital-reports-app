package com.example.digital_reports_app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.digital_reports_app.data.local.dao.ReportDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncReportsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reportDao: ReportDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val allReports = reportDao.getAllReports().first()
            val pendingReports = allReports.filter { it.status == "PENDIENTE" }

            for (report in pendingReports) {
                // Simulate/execute background sync processing
                reportDao.updateReportStatus(report.reportId, "EN_PROCESO")
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
