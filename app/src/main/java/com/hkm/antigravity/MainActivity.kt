package com.hkm.antigravity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hkm.antigravity.data.repository.AccountRepository
import com.hkm.antigravity.ui.screens.MainScreen
import com.hkm.antigravity.ui.theme.AntigravityTheme

class MainActivity : ComponentActivity() {
    private lateinit var accountRepository: AccountRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        accountRepository = AccountRepository(applicationContext)

        setContent {
            AntigravityTheme {
                MainScreen(
                    repository = accountRepository,
                    onFinish = { finish() }
                )
            }
        }
    }
}
