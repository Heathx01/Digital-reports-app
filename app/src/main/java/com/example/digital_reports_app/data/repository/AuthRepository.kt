package com.example.digital_reports_app.data.repository

import com.example.digital_reports_app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(userCode: String, password: String): Result<UserEntity>
    fun getCurrentUserId(): Flow<String?>
    fun getCurrentUserName(): Flow<String?>
    suspend fun logout()
    suspend fun getUserById(userId: String): UserEntity?
}
