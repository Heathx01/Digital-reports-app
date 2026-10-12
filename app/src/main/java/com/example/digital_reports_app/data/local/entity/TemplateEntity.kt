package com.example.digital_reports_app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val templateId: String,
    val title: String,
    val category: String,
    val description: String,
    val fieldsJson: String,
    val backgroundPath: String? = null,
    val timestamp: Long
)
