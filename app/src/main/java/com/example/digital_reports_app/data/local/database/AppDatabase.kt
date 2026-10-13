package com.example.digital_reports_app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.digital_reports_app.data.local.dao.ReportDao
import com.example.digital_reports_app.data.local.dao.TemplateDao
import com.example.digital_reports_app.data.local.dao.UserDao
import com.example.digital_reports_app.data.local.entity.ReportEntity
import com.example.digital_reports_app.data.local.entity.TemplateEntity
import com.example.digital_reports_app.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, ReportEntity::class, TemplateEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun reportDao(): ReportDao
    abstract fun templateDao(): TemplateDao
}
