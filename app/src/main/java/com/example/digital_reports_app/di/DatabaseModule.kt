package com.example.digital_reports_app.di

import android.content.Context
import androidx.room.Room
import com.example.digital_reports_app.data.local.dao.ReportDao
import com.example.digital_reports_app.data.local.dao.TemplateDao
import com.example.digital_reports_app.data.local.dao.UserDao
import com.example.digital_reports_app.data.local.database.AppDatabase
import com.example.digital_reports_app.data.local.entity.UserEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "digital_reports.db"
        )
            .fallbackToDestructiveMigration()
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            if (db.userDao().getUserByCode("215800") == null) {
                db.userDao().insertUsers(
                    listOf(
                        UserEntity(
                            userId = "admin_215800",
                            userCode = "215800",
                            name = "Administrador",
                            passwordHash = "Prd12345#",
                            role = "ADMIN",
                            shift = "DIURNO",
                            isAssignedToDevice = true
                        )
                    )
                )
            }
        }

        return db
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideReportDao(database: AppDatabase): ReportDao {
        return database.reportDao()
    }

    @Provides
    fun provideTemplateDao(database: AppDatabase): TemplateDao {
        return database.templateDao()
    }
}
