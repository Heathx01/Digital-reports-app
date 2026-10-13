package com.example.digital_reports_app.util

import java.security.MessageDigest

object SecurityUtils {

    fun hashPassword(password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            password
        }
    }

    fun verifyPassword(inputPassword: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return false
        val inputHash = hashPassword(inputPassword)
        return inputHash == storedHash || inputPassword == storedHash
    }
}
