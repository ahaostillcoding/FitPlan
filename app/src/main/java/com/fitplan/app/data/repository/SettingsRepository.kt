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
    val selectedWorkoutDayId: Long? = null,
    val workoutDraftJson: String = ""
)

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun saveDeepSeekApiKey(apiKey: String)
    suspend fun saveDeepSeekModel(model: String)
    suspend fun clearDeepSeekApiKey()
    suspend fun saveSelectedWorkoutDayId(dayId: Long?)
    suspend fun saveWorkoutDraftJson(json: String)
    suspend fun clearWorkoutDraft()
}

class DataStoreSettingsRepository(
    private val appContext: Context
) : SettingsRepository {
    private object Keys {
        val deepSeekApiKey = stringPreferencesKey("deepseek_api_key")
        val deepSeekModel = stringPreferencesKey("deepseek_model")
        val selectedWorkoutDayId = stringPreferencesKey("selected_workout_day_id")
        val workoutDraftJson = stringPreferencesKey("workout_draft_json")
    }

    override val settings: Flow<AppSettings> = appContext.settingsDataStore.data.map { preferences ->
        AppSettings(
            deepSeekApiKey = preferences[Keys.deepSeekApiKey].orEmpty(),
            deepSeekModel = preferences[Keys.deepSeekModel].orEmpty().ifBlank { DEFAULT_DEEPSEEK_MODEL },
            selectedWorkoutDayId = preferences[Keys.selectedWorkoutDayId]?.toLongOrNull(),
            workoutDraftJson = preferences[Keys.workoutDraftJson].orEmpty()
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

    override suspend fun saveWorkoutDraftJson(json: String) {
        appContext.settingsDataStore.edit { preferences ->
            if (json.isBlank()) {
                preferences.remove(Keys.workoutDraftJson)
            } else {
                preferences[Keys.workoutDraftJson] = json
            }
        }
    }

    override suspend fun clearWorkoutDraft() {
        appContext.settingsDataStore.edit { preferences ->
            preferences.remove(Keys.workoutDraftJson)
        }
    }
}
