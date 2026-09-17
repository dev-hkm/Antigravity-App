package com.hkm.antigravity

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.hkm.antigravity.data.repository.AccountRepository
import com.hkm.antigravity.data.repository.SettingsRepository
import com.hkm.antigravity.ui.screens.MainScreen
import com.hkm.antigravity.ui.theme.AntigravityTheme

class MainActivity : ComponentActivity() {
    private lateinit var accountRepository: AccountRepository
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        accountRepository = AccountRepository(applicationContext)
        settingsRepository = SettingsRepository(applicationContext)

        setContent {
            val settings by settingsRepository.settingsFlow.collectAsState()

            AntigravityTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor
            ) {
                MainScreen(
                    accountRepository = accountRepository,
                    settingsRepository = settingsRepository,
                    onFinish = { finish() }
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onStop() {
        super.onStop()
        CookieManager.getInstance().flush()
    }
}
