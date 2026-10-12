package com.example.digital_reports_app.data.repository

import com.example.digital_reports_app.data.local.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun getAllTemplates(): Flow<List<TemplateEntity>>
    suspend fun insertTemplate(template: TemplateEntity)
    suspend fun deleteTemplate(templateId: String)
    suspend fun deleteTemplatesByIds(templateIds: List<String>)
}
