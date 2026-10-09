package com.example.digital_reports_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.digital_reports_app.data.local.entity.ReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {

    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE authorCode = :authorCode ORDER BY timestamp DESC")
    fun getReportsByAuthor(authorCode: String): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE reportId = :reportId LIMIT 1")
    fun getReportById(reportId: String): Flow<ReportEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("UPDATE reports SET status = :status WHERE reportId = :reportId")
    suspend fun updateReportStatus(reportId: String, status: String)

    @Query("DELETE FROM reports WHERE reportId = :reportId")
    suspend fun deleteReport(reportId: String)
}
