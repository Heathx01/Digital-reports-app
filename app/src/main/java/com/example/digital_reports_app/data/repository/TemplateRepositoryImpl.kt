package com.example.digital_reports_app.data.repository

import com.example.digital_reports_app.data.local.dao.TemplateDao
import com.example.digital_reports_app.data.local.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepositoryImpl @Inject constructor(
    private val templateDao: TemplateDao
) : TemplateRepository {

    override fun getAllTemplates(): Flow<List<TemplateEntity>> {
        return templateDao.getAllTemplates()
    }

    override suspend fun insertTemplate(template: TemplateEntity) {
        templateDao.insertTemplate(template)
    }

    override suspend fun deleteTemplate(templateId: String) {
        templateDao.deleteTemplate(templateId)
    }

    override suspend fun deleteTemplatesByIds(templateIds: List<String>) {
        templateDao.deleteTemplatesByIds(templateIds)
    }
}
