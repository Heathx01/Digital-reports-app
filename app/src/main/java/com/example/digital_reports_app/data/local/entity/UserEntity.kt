package com.example.digital_reports_app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val userCode: String,
    val name: String,
    val passwordHash: String,
    val role: String = "ADMIN",
    val shift: String = "DIURNO",
    val isAssignedToDevice: Boolean = true
)
