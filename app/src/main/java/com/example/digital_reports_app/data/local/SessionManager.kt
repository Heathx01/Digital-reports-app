package com.example.digital_reports_app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_session")

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_USER_ID = stringPreferencesKey("current_user_id")
        private val KEY_USER_NAME = stringPreferencesKey("current_user_name")
        private val KEY_USER_CODE = stringPreferencesKey("current_user_code")
        private val KEY_IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        private val KEY_LANGUAGE = stringPreferencesKey("app_language")
    }

    val currentUserId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_ID]
    }

    val currentUserName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_NAME]
    }

    val currentUserCode: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_CODE]
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_DARK_THEME] ?: false
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_LANGUAGE] ?: "Español"
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_DARK_THEME] = isDark
        }
    }

    suspend fun setAppLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = language
        }
    }

    suspend fun saveSession(userId: String, name: String, userCode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_ID] = userId
            preferences[KEY_USER_NAME] = name
            preferences[KEY_USER_CODE] = userCode
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
