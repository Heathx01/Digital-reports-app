package com.example.digital_reports_app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val reportId: String,
    val title: String,
    val category: String,
    val description: String,
    val status: String = "PENDIENTE",
    val dateFormatted: String,
    val timestamp: Long,
    val authorCode: String,
    val authorName: String
)
