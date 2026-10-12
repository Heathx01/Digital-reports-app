package com.example.digital_reports_app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.digital_reports_app.data.local.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    @Query("SELECT * FROM templates ORDER BY timestamp DESC")
    fun getAllTemplates(): Flow<List<TemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TemplateEntity)

    @Query("DELETE FROM templates WHERE templateId = :templateId")
    suspend fun deleteTemplate(templateId: String)

    @Query("DELETE FROM templates WHERE templateId IN (:templateIds)")
    suspend fun deleteTemplatesByIds(templateIds: List<String>)
}
