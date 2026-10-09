package com.example.digital_reports_app.domain.model

data class User(
    val userId: String,
    val email: String,
    val name: String,
    val shift: String = "DIURNO",
    val isAssignedToDevice: Boolean = true
)
