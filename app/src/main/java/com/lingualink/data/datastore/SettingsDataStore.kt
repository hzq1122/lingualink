package com.lingualink.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        private val API_ENDPOINT = stringPreferencesKey("api_endpoint")
        private val API_KEY = stringPreferencesKey("api_key")
        private val SELECTED_MODEL = stringPreferencesKey("selected_model")
        private val CHECK_UPDATES = booleanPreferencesKey("check_updates")
    }

    val apiEndpoint: Flow<String> = context.settingsDataStore.data.map { it[API_ENDPOINT] ?: "" }
    val apiKey: Flow<String> = context.settingsDataStore.data.map { it[API_KEY] ?: "" }
    val selectedModel: Flow<String> = context.settingsDataStore.data.map { it[SELECTED_MODEL] ?: "deepseek-chat" }
    val checkUpdates: Flow<Boolean> = context.settingsDataStore.data.map { it[CHECK_UPDATES] ?: true }

    suspend fun saveApiSettings(endpoint: String, key: String, model: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[API_ENDPOINT] = endpoint
            prefs[API_KEY] = key
            prefs[SELECTED_MODEL] = model
        }
    }

    suspend fun setCheckUpdates(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[CHECK_UPDATES] = enabled }
    }

    suspend fun getApiEndpoint(): String = apiEndpoint.first()
    suspend fun getApiKey(): String = apiKey.first()
    suspend fun getSelectedModel(): String = selectedModel.first()
    suspend fun getCheckUpdates(): Boolean = checkUpdates.first()
}
