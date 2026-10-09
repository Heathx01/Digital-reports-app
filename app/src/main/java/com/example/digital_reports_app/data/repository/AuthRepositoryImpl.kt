package com.example.digital_reports_app.data.repository

import com.example.digital_reports_app.data.local.SessionManager
import com.example.digital_reports_app.data.local.dao.UserDao
import com.example.digital_reports_app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun login(userCode: String, password: String): Result<UserEntity> {
        return try {
            val user = userDao.getUserByCode(userCode.trim())
            if (user == null) {
                Result.failure(Exception("Código de usuario no encontrado."))
            } else if (user.passwordHash != password) {
                Result.failure(Exception("Contraseña incorrecta."))
            } else {
                sessionManager.saveSession(user.userId, user.name, user.userCode)
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUserId(): Flow<String?> {
        return sessionManager.currentUserId
    }

    override fun getCurrentUserName(): Flow<String?> {
        return sessionManager.currentUserName
    }

    override suspend fun logout() {
        sessionManager.clearSession()
    }

    override suspend fun getUserById(userId: String): UserEntity? {
        return userDao.getUserById(userId)
    }
}
