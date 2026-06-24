package com.fitplan.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val SETTINGS_DATASTORE_NAME = "fitplan_settings"
const val DEFAULT_DEEPSEEK_MODEL = "deepseek-v4-flash"

private val Context.settingsDataStore by preferencesDataStore(
    name = SETTINGS_DATASTORE_NAME
)

data class AppSettings(
    val deepSeekApiKey: String = "",
    val deepSeekModel: String = DEFAULT_DEEPSEEK_MODEL,
    val selectedWorkoutDayId: Long? = null
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun saveDeepSeekApiKey(apiKey: String)
    suspend fun saveDeepSeekModel(model: String)
    suspend fun clearDeepSeekApiKey()
    suspend fun saveSelectedWorkoutDayId(dayId: Long?)
}

class DataStoreSettingsRepository(
    private val appContext: Context
) : SettingsRepository {
    private object Keys {
        val deepSeekApiKey = stringPreferencesKey("deepseek_api_key")
        val deepSeekModel = stringPreferencesKey("deepseek_model")
        val selectedWorkoutDayId = stringPreferencesKey("selected_workout_day_id")
    }

    override val settings: Flow<AppSettings> = appContext.settingsDataStore.data.map { preferences ->
        AppSettings(
            deepSeekApiKey = preferences[Keys.deepSeekApiKey].orEmpty(),
            deepSeekModel = preferences[Keys.deepSeekModel].orEmpty().ifBlank { DEFAULT_DEEPSEEK_MODEL },
            selectedWorkoutDayId = preferences[Keys.selectedWorkoutDayId]?.toLongOrNull()
        )
    }

    override suspend fun saveDeepSeekApiKey(apiKey: String) {
        appContext.settingsDataStore.edit { preferences ->
            preferences[Keys.deepSeekApiKey] = apiKey.trim()
        }
    }

    override suspend fun saveDeepSeekModel(model: String) {
        appContext.settingsDataStore.edit { preferences ->
            preferences[Keys.deepSeekModel] = model.trim().ifBlank { DEFAULT_DEEPSEEK_MODEL }
        }
    }

    override suspend fun clearDeepSeekApiKey() {
        appContext.settingsDataStore.edit { preferences ->
            preferences.remove(Keys.deepSeekApiKey)
        }
    }

    override suspend fun saveSelectedWorkoutDayId(dayId: Long?) {
        appContext.settingsDataStore.edit { preferences ->
            if (dayId == null || dayId <= 0) {
                preferences.remove(Keys.selectedWorkoutDayId)
            } else {
                preferences[Keys.selectedWorkoutDayId] = dayId.toString()
            }
        }
    }
}
