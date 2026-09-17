package com.hkm.antigravity.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hkm.antigravity.data.model.Account
import com.hkm.antigravity.data.repository.AccountRepository
import com.hkm.antigravity.data.repository.SettingsRepository
import com.hkm.antigravity.ui.components.AccountBottomSheet
import com.hkm.antigravity.ui.components.AccountDialog
import com.hkm.antigravity.ui.components.DraggableActionPill
import com.hkm.antigravity.ui.components.SettingsBottomSheet
import com.hkm.antigravity.ui.components.WebViewContainer
import com.hkm.antigravity.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    accountRepository: AccountRepository,
    settingsRepository: SettingsRepository,
    onFinish: () -> Unit
) {
    val accounts by accountRepository.accountsFlow.collectAsState()
    val activeAccount by accountRepository.activeAccountFlow.collectAsState()
    val settings by settingsRepository.settingsFlow.collectAsState()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(activeAccount?.url ?: AccountRepository.DEFAULT_REMOTE_URL) }
    var pageProgress by remember { mutableIntStateOf(0) }

    var showAccountSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }

    // File / Media Chooser Support
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uris: Array<Uri>? = if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            when {
                data?.clipData != null -> {
                    val count = data.clipData!!.itemCount
                    Array(count) { i -> data.clipData!!.getItemAt(i).uri }
                }
                data?.data != null -> arrayOf(data.data!!)
                else -> null
            }
        } else null

        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    // Intercept back button for history navigation
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onFinish()
        }
    }

    val activeColor = remember(activeAccount) {
        try {
            Color(android.graphics.Color.parseColor(activeAccount?.colorHex ?: "#3b82f6"))
        } catch (e: Exception) {
            AccentBlue
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val accountId = activeAccount?.id ?: "acc_primary"
        val targetUrl = activeAccount?.url ?: AccountRepository.DEFAULT_REMOTE_URL

        // Web view taking 100% full screen with IME & navigation bar padding
        key(accountId) {
            WebViewContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .navigationBarsPadding()
                    .statusBarsPadding(),
                accountId = accountId,
                url = targetUrl,
                appThemeMode = settings.themeMode,
                webTheme = settings.webTheme,
                isDesktopMode = settings.isDesktopMode,
                autoConnect = settings.autoConnect,
                onWebViewCreated = { webView ->
                    webViewInstance = webView
                },
                onProgressChange = { pageProgress = it },
                onTitleChange = { /* Title update */ },
                onUrlChange = { url ->
                    currentUrl = url
                },
                onShowFileChooser = { callback, params ->
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = callback

                    val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    }
                    try {
                        filePickerLauncher.launch(intent)
                        true
                    } catch (e: Exception) {
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = null
                        false
                    }
                }
            )
        }

        // Hairline loading indicator at the very top
        if (pageProgress in 1..99) {
            LinearProgressIndicator(
                progress = { pageProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent
            )
        }

        // Draggable & Dockable Action Pill with deep customization
        DraggableActionPill(
            accountColor = activeColor,
            isDesktopMode = settings.isDesktopMode,
            webTheme = settings.webTheme,
            orientation = settings.pillOrientation,
            scale = settings.pillScale,
            showAccount = settings.showAccountOnPill,
            showDesktop = settings.showDesktopOnPill,
            showWebTheme = settings.showWebThemeOnPill,
            showReload = settings.showReloadOnPill,
            showSettings = settings.showSettingsOnPill,
            onAccountClick = { showAccountSheet = true },
            onToggleDesktopMode = {
                settingsRepository.setDesktopMode(!settings.isDesktopMode)
            },
            onToggleWebTheme = {
                val next = if (settings.webTheme == "dark") "light" else "dark"
                settingsRepository.setWebTheme(next)
            },
            onReloadClick = { webViewInstance?.reload() },
            onSettingsClick = { showSettingsSheet = true }
        )
    }

    // Account Switcher Bottom Sheet
    if (showAccountSheet) {
        AccountBottomSheet(
            accounts = accounts,
            activeAccount = activeAccount,
            onDismiss = { showAccountSheet = false },
            onSelectAccount = { target ->
                val newAcc = accountRepository.switchAccount(target.id)
                if (newAcc != null) {
                    webViewInstance?.loadUrl(newAcc.url)
                }
            },
            onAddAccountClick = {
                accountToEdit = null
                showAccountDialog = true
            },
            onEditAccountClick = { acc ->
                accountToEdit = acc
                showAccountDialog = true
            }
        )
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        SettingsBottomSheet(
            settings = settings,
            onDismiss = { showSettingsSheet = false },
            onUpdateAutoConnect = { enabled ->
                settingsRepository.setAutoConnect(enabled)
            },
            onUpdateThemeMode = { mode ->
                settingsRepository.setThemeMode(mode)
            },
            onUpdateWebTheme = { mode ->
                settingsRepository.setWebTheme(mode)
            },
            onUpdateDynamicColor = { enabled ->
                settingsRepository.setDynamicColor(enabled)
            },
            onUpdateDesktopMode = { enabled ->
                settingsRepository.setDesktopMode(enabled)
            },
            onUpdatePillOrientation = { orientation ->
                settingsRepository.setPillOrientation(orientation)
            },
            onUpdatePillScale = { scale ->
                settingsRepository.setPillScale(scale)
            },
            onUpdateToolbarButtons = { showAcc, showDesk, showWeb, showRel, showSet ->
                settingsRepository.setToolbarButtons(showAcc, showDesk, showWeb, showRel, showSet)
            },
            onClearCache = {
                webViewInstance?.clearCache(true)
            }
        )
    }

    // Account Add / Edit Dialog
    if (showAccountDialog) {
        AccountDialog(
            accountToEdit = accountToEdit,
            canDelete = accounts.size > 1,
            onDismiss = { showAccountDialog = false },
            onSave = { name, colorHex, url ->
                if (accountToEdit != null) {
                    accountRepository.updateAccount(accountToEdit!!.id, name, colorHex, url)
                } else {
                    accountRepository.createAccount(name, colorHex, url)
                }
                showAccountDialog = false
            },
            onDelete = {
                if (accountToEdit != null) {
                    accountRepository.deleteAccount(accountToEdit!!.id)
                }
                showAccountDialog = false
            }
        )
    }
}
