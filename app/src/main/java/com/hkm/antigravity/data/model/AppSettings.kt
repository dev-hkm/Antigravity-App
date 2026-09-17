package com.hkm.antigravity.data.model

data class AppSettings(
    val autoConnect: Boolean = true,
    val themeMode: String = "system", // "system", "dark", "light"
    val webTheme: String = "dark", // "dark", "light", "follow_app"
    val dynamicColor: Boolean = true,
    val isDesktopMode: Boolean = false,
    val defaultZenMode: Boolean = true,
    // Toolbar Customization
    val pillOrientation: String = "horizontal", // "horizontal", "vertical"
    val pillScale: String = "standard", // "compact", "standard", "large"
    val showAccountOnPill: Boolean = true,
    val showDesktopOnPill: Boolean = true,
    val showWebThemeOnPill: Boolean = true,
    val showReloadOnPill: Boolean = true,
    val showSettingsOnPill: Boolean = true
)
