package com.messenger.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.messenger.core.common.logger.logD
import com.messenger.core.common.logger.logE
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * DataStore для хранения настроек приложения.
 * Используется для: темы, уведомлений, сессии пользователя.
 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "app_settings"
)

/**
 * Ключи предпочтений.
 */
object PreferencesKeys {
    val THEME_MODE = intPreferencesKey("theme_mode") // 0=System, 1=Light, 2=Dark
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    val USER_ID = stringPreferencesKey("user_id")
    val USER_TOKEN = stringPreferencesKey("user_token")
    val USER_REFRESH_TOKEN = stringPreferencesKey("user_refresh_token")
    val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
    val LANGUAGE_CODE = stringPreferencesKey("language_code")
}

/**
 * Менеджер настроек приложения.
 * Обеспечивает типобезопасный доступ к DataStore.
 */
class AppSettingsManager(private val context: Context) {

    /**
     * Поток режима темы.
     * @return Flow<Int> где 0=System, 1=Light, 2=Dark
     */
    val themeModeFlow: Flow<Int> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                logE("DataStore", "Ошибка чтения темы", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.THEME_MODE] ?: 0 // По умолчанию System
        }

    /**
     * Поток состояния уведомлений.
     */
    val notificationsEnabledFlow: Flow<Boolean> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
        }

    /**
     * Поток состояния биометрической аутентификации.
     */
    val biometricEnabledFlow: Flow<Boolean> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] ?: false
        }

    /**
     * Поток ID пользователя.
     */
    val userIdFlow: Flow<String?> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.USER_ID]
        }

    /**
     * Поток токена доступа.
     */
    val userTokenFlow: Flow<String?> = context.settingsDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.USER_TOKEN]
        }

    /**
     * Установка режима темы.
     * @param mode 0=System, 1=Light, 2=Dark
     */
    suspend fun setThemeMode(mode: Int) {
        logD("Settings", "Установка темы: $mode")
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    /**
     * Включение/отключение уведомлений.
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        logD("Settings", "Уведомления: $enabled")
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * Включение/отключение биометрической аутентификации.
     */
    suspend fun setBiometricEnabled(enabled: Boolean) {
        logD("Settings", "Биометрия: $enabled")
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.BIOMETRIC_ENABLED] = enabled
        }
    }

    /**
     * Сохранение сессии пользователя.
     */
    suspend fun saveSession(userId: String, token: String, refreshToken: String) {
        logD("Settings", "Сохранение сессии для пользователя: $userId")
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = userId
            preferences[PreferencesKeys.USER_TOKEN] = token
            preferences[PreferencesKeys.USER_REFRESH_TOKEN] = refreshToken
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    /**
     * Очистка сессии (logout).
     */
    suspend fun clearSession() {
        logD("Settings", "Очистка сессии")
        context.settingsDataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.USER_ID)
            preferences.remove(PreferencesKeys.USER_TOKEN)
            preferences.remove(PreferencesKeys.USER_REFRESH_TOKEN)
        }
    }

    /**
     * Получение текущего токена (не Flow версия).
     */
    suspend fun getToken(): String? {
        return context.settingsDataStore.data
            .map { it[PreferencesKeys.USER_TOKEN] }
            .firstOrNull()
    }

    /**
     * Проверка авторизации пользователя.
     */
    suspend fun isAuthorized(): Boolean {
        return !getToken().isNullOrBlank()
    }

    /**
     * Обновление timestamp последней синхронизации.
     */
    suspend fun updateLastSyncTimestamp() {
        context.settingsDataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = System.currentTimeMillis()
        }
    }
}

/**
 * Extension функция для создания менеджера настроек.
 */
fun Context.createSettingsManager(): AppSettingsManager = AppSettingsManager(this)
