package com.yaycheckmate.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yaycheckmate_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val MASCOT_ENABLED = booleanPreferencesKey("mascot_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val USERNAME = stringPreferencesKey("username")
    }

    val isOnboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_DONE] ?: false
    }

    val isMascotEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[MASCOT_ENABLED] ?: true
    }

    val isVibrationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[VIBRATION_ENABLED] ?: true
    }

    val username: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USERNAME] ?: "Detective"
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { prefs -> prefs[ONBOARDING_DONE] = done }
    }

    suspend fun setMascotEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[MASCOT_ENABLED] = enabled }
    }

    suspend fun setUsername(name: String) {
        context.dataStore.edit { prefs -> prefs[USERNAME] = name }
    }
}
