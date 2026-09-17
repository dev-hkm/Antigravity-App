package com.hkm.antigravity.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.hkm.antigravity.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("antigravity_settings_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _settingsFlow = MutableStateFlow(AppSettings())
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val json = prefs.getString(KEY_SETTINGS, null)
        val loaded = if (!json.isNullOrEmpty()) {
            try {
                gson.fromJson(json, AppSettings::class.java) ?: AppSettings()
            } catch (e: Exception) {
                AppSettings()
            }
        } else {
            AppSettings()
        }
        _settingsFlow.value = loaded
    }

    private fun saveSettings(settings: AppSettings) {
        _settingsFlow.value = settings
        prefs.edit().putString(KEY_SETTINGS, gson.toJson(settings)).apply()
    }

    fun setAutoConnect(enabled: Boolean) {
        saveSettings(_settingsFlow.value.copy(autoConnect = enabled))
    }

    fun setThemeMode(mode: String) {
        saveSettings(_settingsFlow.value.copy(themeMode = mode))
    }

    fun setDefaultZenMode(enabled: Boolean) {
        saveSettings(_settingsFlow.value.copy(defaultZenMode = enabled))
    }

    companion object {
        private const val KEY_SETTINGS = "key_app_settings"
    }
}
