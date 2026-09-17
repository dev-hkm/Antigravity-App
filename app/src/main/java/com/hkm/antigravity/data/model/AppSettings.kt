package com.hkm.antigravity.data.model

data class AppSettings(
    val autoConnect: Boolean = true,
    val themeMode: String = "system", // "system", "dark", "light"
    val defaultZenMode: Boolean = false
)
