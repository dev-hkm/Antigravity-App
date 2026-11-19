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
        _settingsFlow.value = loaded.normalized()
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

    fun setWebTheme(mode: String) {
        saveSettings(_settingsFlow.value.copy(webTheme = mode))
    }

    fun setDynamicColor(enabled: Boolean) {
        saveSettings(_settingsFlow.value.copy(dynamicColor = enabled))
    }

    fun setDesktopMode(enabled: Boolean) {
        saveSettings(_settingsFlow.value.copy(isDesktopMode = enabled))
    }

    fun setDefaultZenMode(enabled: Boolean) {
        saveSettings(_settingsFlow.value.copy(defaultZenMode = enabled))
    }

    fun setKeepScreenOn(enabled: Boolean) {
        saveSettings(_settingsFlow.value.copy(keepScreenOn = enabled))
    }

    fun setEnableNotifications(enabled: Boolean) {
        saveSettings(_settingsFlow.value.copy(enableNotifications = enabled))
    }

    fun setPillOrientation(orientation: String) {
        saveSettings(_settingsFlow.value.copy(pillOrientation = orientation))
    }

    fun setPillScale(scale: String) {
        saveSettings(_settingsFlow.value.copy(pillScale = scale))
    }

    fun setPillOpacity(opacity: Float) {
        saveSettings(_settingsFlow.value.copy(pillOpacity = opacity.coerceIn(0.35f, 1f)))
    }

    fun setToolbarButtons(
        showAccount: Boolean = _settingsFlow.value.showAccountOnPill,
        showDesktop: Boolean = _settingsFlow.value.showDesktopOnPill,
        showWebTheme: Boolean = _settingsFlow.value.showWebThemeOnPill,
        showReload: Boolean = _settingsFlow.value.showReloadOnPill,
        showSettings: Boolean = _settingsFlow.value.showSettingsOnPill
    ) {
        saveSettings(_settingsFlow.value.copy(
            showAccountOnPill = showAccount,
            showDesktopOnPill = showDesktop,
            showWebThemeOnPill = showWebTheme,
            showReloadOnPill = showReload,
            showSettingsOnPill = showSettings
        ))
    }

    private fun AppSettings.normalized(): AppSettings {
        val safeThemeMode = runCatching { themeMode }.getOrNull()
            ?.takeIf { it in setOf("system", "dark", "light") } ?: "system"
        val safeWebTheme = runCatching { webTheme }.getOrNull()
            ?.takeIf { it in setOf("dark", "light", "follow_app") } ?: "dark"
        val safeOrientation = runCatching { pillOrientation }.getOrNull()
            ?.takeIf { it in setOf("horizontal", "vertical") } ?: "horizontal"
        val safeScale = runCatching { pillScale }.getOrNull()
            ?.takeIf { it in setOf("compact", "standard", "large") } ?: "standard"
        val safeOpacity = runCatching { pillOpacity.coerceIn(0.35f, 1f) }.getOrDefault(0.90f)
        return copy(
            themeMode = safeThemeMode,
            webTheme = safeWebTheme,
            pillOrientation = safeOrientation,
            pillScale = safeScale,
            pillOpacity = safeOpacity
        )
    }

    companion object {
        private const val KEY_SETTINGS = "key_app_settings"
    }
}
