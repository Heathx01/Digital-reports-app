package com.example.digital_reports_app.di

import android.content.Context
import androidx.room.Room
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
        ).build()

        CoroutineScope(Dispatchers.IO).launch {
            if (db.userDao().getUserByEmail("ejemplo@correo.com") == null) {
                db.userDao().insertUsers(
                    listOf(
                        UserEntity(
                            userId = "usr_001",
                            email = "ejemplo@correo.com",
                            name = "Juan Pérez",
                            passwordHash = "123456",
                            shift = "DIURNO",
                            isAssignedToDevice = true
                        ),
                        UserEntity(
                            userId = "usr_002",
                            email = "sandeep@empresa.com",
                            name = "Sandeep Jadhav",
                            passwordHash = "123456",
                            shift = "DIURNO",
                            isAssignedToDevice = true
                        ),
                        UserEntity(
                            userId = "usr_003",
                            email = "prakash@empresa.com",
                            name = "Prakash Sharma",
                            passwordHash = "123456",
                            shift = "NOCTURNO",
                            isAssignedToDevice = true
                        ),
                        UserEntity(
                            userId = "usr_004",
                            email = "mahesh@empresa.com",
                            name = "Mahesh Patil",
                            passwordHash = "123456",
                            shift = "NOCTURNO",
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
}
