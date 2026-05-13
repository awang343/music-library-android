package com.musiclib.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class Settings(
    val serverUrl: String,
    val authToken: String,
) {
    val isConfigured: Boolean get() = serverUrl.isNotBlank()
}

class SettingsRepository(private val context: Context) {
    private object Keys {
        val SERVER_URL: Preferences.Key<String> = stringPreferencesKey("server_url")
        val AUTH_TOKEN: Preferences.Key<String> = stringPreferencesKey("auth_token")
    }

    val flow: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            serverUrl = prefs[Keys.SERVER_URL].orEmpty(),
            authToken = prefs[Keys.AUTH_TOKEN].orEmpty(),
        )
    }

    suspend fun save(settings: Settings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_URL] = settings.serverUrl.trim()
            prefs[Keys.AUTH_TOKEN] = settings.authToken.trim()
        }
    }
}
