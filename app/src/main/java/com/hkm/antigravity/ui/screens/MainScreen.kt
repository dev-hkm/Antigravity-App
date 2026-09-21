package com.hkm.antigravity.ui.screens

import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.hkm.antigravity.ui.components.flushCookiesForWebView
import com.hkm.antigravity.ui.components.WebViewContainer
import android.app.Activity
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
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
    val context = LocalContext.current

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webViewGeneration by remember { mutableIntStateOf(0) }
    var currentUrl by remember { mutableStateOf(activeAccount?.url ?: AccountRepository.DEFAULT_REMOTE_URL) }
    var pageProgress by remember { mutableIntStateOf(0) }
    var googleAuthUrl by remember { mutableStateOf<String?>(null) }
    // Access tokens stay in process memory and are never written to storage.
    var initialAccessToken by remember { mutableStateOf<String?>(null) }

    var showAccountSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, webViewInstance) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                flushCookiesForWebView(webViewInstance)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

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

    fun acceptNativeAuthorization(accessToken: String?) {
        if (accessToken.isNullOrBlank()) {
            Toast.makeText(context, "Google did not return an access token. Try again.", Toast.LENGTH_LONG).show()
            return
        }
        googleAuthUrl = null
        initialAccessToken = accessToken
        webViewInstance = null
        webViewGeneration++
    }

    val nativeAuthorizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val token = result.data?.let { data ->
            runCatching {
                Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data).accessToken
            }.getOrNull()
        }
        acceptNativeAuthorization(token)
    }

    fun requestNativeAuthorization() {
        val activity = context as? Activity
        if (activity == null) {
            Toast.makeText(context, "Unable to start Google sign-in.", Toast.LENGTH_LONG).show()
            return
        }
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope("https://www.googleapis.com/auth/cloud-platform"),
                    Scope("openid"),
                    Scope("https://www.googleapis.com/auth/userinfo.email"),
                    Scope("https://www.googleapis.com/auth/userinfo.profile")
                )
            )
            .build()
        Identity.getAuthorizationClient(activity).authorize(request)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    result.pendingIntent?.let { pendingIntent ->
                        nativeAuthorizationLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                    } ?: acceptNativeAuthorization(null)
                } else {
                    acceptNativeAuthorization(result.accessToken)
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Google sign-in could not start. Try again.", Toast.LENGTH_LONG).show()
            }
    }

    // Intercept back button for history navigation
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onFinish()
        }
    }

    val fallbackAccent = MaterialTheme.colorScheme.primary
    val activeColor = remember(activeAccount, fallbackAccent) {
        try {
            Color(android.graphics.Color.parseColor(activeAccount?.colorHex ?: "#3b82f6"))
        } catch (e: Exception) {
            fallbackAccent
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
            key(accountId, webViewGeneration) {
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
                    initialAccessToken = initialAccessToken,
                    onWebViewCreated = { webView ->
                        webViewInstance = webView
                    },
                    onProgressChange = { pageProgress = it },
                    onTitleChange = { /* Title update */ },
                    onUrlChange = { url ->
                        currentUrl = url
                    },
                    onGoogleAuthRequired = { url ->
                        googleAuthUrl = url
                    },
                    onRendererCrashed = {
                        webViewInstance = null
                        webViewGeneration++
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
                opacity = settings.pillOpacity,
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

    googleAuthUrl?.let {
        AlertDialog(
            onDismissRequest = { googleAuthUrl = null },
            title = { Text("Đăng nhập Google") },
            text = {
                Text(
                    "Antigravity yêu cầu xác thực Google ngoài WebView. " +
                        "Ứng dụng sẽ dùng màn hình đăng nhập Google chính chủ, rồi quay lại ngay phiên hiện tại."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        requestNativeAuthorization()
                    }
                ) {
                    Text("Tiếp tục với Google")
                }
            },
            dismissButton = {
                TextButton(onClick = { googleAuthUrl = null }) {
                    Text("Đóng")
                }
            }
        )
    }

    // Account Switcher Bottom Sheet
    if (showAccountSheet) {
        AccountBottomSheet(
            accounts = accounts,
            activeAccount = activeAccount,
            onDismiss = { showAccountSheet = false },
            onSelectAccount = { target ->
                flushCookiesForWebView(webViewInstance)
                // A short-lived grant belongs only to the account that requested it.
                initialAccessToken = null
                accountRepository.switchAccount(target.id)
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
            onUpdatePillOpacity = { opacity ->
                settingsRepository.setPillOpacity(opacity)
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
                flushCookiesForWebView(webViewInstance)
                if (accountToEdit != null) {
                    accountRepository.updateAccount(accountToEdit!!.id, name, colorHex, url)
                } else {
                    accountRepository.createAccount(name, colorHex, url)
                }
                showAccountDialog = false
            },
            onDelete = {
                if (accountToEdit != null) {
                    flushCookiesForWebView(webViewInstance)
                    accountRepository.deleteAccount(accountToEdit!!.id)
                }
                showAccountDialog = false
            }
        )
    }
}
