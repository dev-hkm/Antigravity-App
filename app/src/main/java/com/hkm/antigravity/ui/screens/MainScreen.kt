package com.hkm.antigravity.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.hkm.antigravity.browser.GeckoRuntimeHolder
import com.hkm.antigravity.data.model.Account
import com.hkm.antigravity.data.repository.AccountRepository
import com.hkm.antigravity.data.repository.SettingsRepository
import com.hkm.antigravity.ui.components.AccountBottomSheet
import com.hkm.antigravity.ui.components.AccountDialog
import com.hkm.antigravity.ui.components.BrowserController
import com.hkm.antigravity.ui.components.DraggableActionPill
import com.hkm.antigravity.ui.components.GeckoBrowserContainer
import com.hkm.antigravity.ui.theme.AppMotion
import com.hkm.antigravity.ui.components.SettingsBottomSheet
import com.hkm.antigravity.ui.theme.*
import org.mozilla.geckoview.GeckoResult

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    var browserController by remember { mutableStateOf<BrowserController?>(null) }
    var browserGeneration by remember { mutableIntStateOf(0) }
    var currentUrl by remember { mutableStateOf(activeAccount?.url ?: AccountRepository.DEFAULT_REMOTE_URL) }
    val pageProgressState = remember { mutableIntStateOf(0) }

    var showAccountSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }

    DisposableEffect(settings.keepScreenOn) {
        val window = (context as? android.app.Activity)?.window
        if (settings.keepScreenOn) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var pendingActivityResult by remember { mutableStateOf<GeckoResult<Intent>?>(null) }
    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pending = pendingActivityResult
        pendingActivityResult = null
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pending?.complete(result.data)
        } else {
            pending?.completeExceptionally(Exception("Cancelled"))
        }
    }

    DisposableEffect(Unit) {
        GeckoRuntimeHolder.setActivityLauncher { pendingIntent ->
            val result = GeckoResult<Intent>()
            pendingActivityResult?.completeExceptionally(Exception("Superseded"))
            pendingActivityResult = result
            runCatching {
                intentSenderLauncher.launch(
                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                )
            }.onFailure { err ->
                pendingActivityResult = null
                result.completeExceptionally(err)
            }
            result
        }
        onDispose {
            GeckoRuntimeHolder.setActivityLauncher(null)
            pendingActivityResult?.completeExceptionally(Exception("Disposed"))
            pendingActivityResult = null
        }
    }

    // File / Media Chooser Support
    var pendingFileCallback by remember { mutableStateOf<((Array<Uri>?) -> Unit)?>(null) }

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

        pendingFileCallback?.invoke(uris)
        pendingFileCallback = null
    }

    // Intercept back button for history navigation
    BackHandler {
        if (browserController?.canGoBack() == true) {
            browserController?.goBack()
        } else {
            onFinish()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val accountId = activeAccount?.id ?: "acc_primary"
        val targetUrl = activeAccount?.url ?: AccountRepository.DEFAULT_REMOTE_URL

        // Embedded Gecko browser taking full screen with settled IME target padding to avoid
        // per-frame SurfaceView resize storms during keyboard / Recents transitions.
        val settledImeModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val density = LocalDensity.current
            val imeTargetBottomPx = WindowInsets.imeAnimationTarget.getBottom(density)
            val navBottomPx = WindowInsets.navigationBars.getBottom(density)
            val settledImeDp = with(density) { maxOf(0, imeTargetBottomPx - navBottomPx).toDp() }
            Modifier.padding(bottom = settledImeDp)
        } else {
            Modifier.imePadding()
        }

        key(accountId, browserGeneration) {
            GeckoBrowserContainer(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .statusBarsPadding()
                    .then(settledImeModifier),
                accountId = accountId,
                url = targetUrl,
                appThemeMode = settings.themeMode,
                webTheme = settings.webTheme,
                isDesktopMode = settings.isDesktopMode,
                autoConnect = settings.autoConnect,
                onControllerReady = { controller ->
                    browserController = controller
                },
                onProgressChange = { pageProgressState.intValue = it },
                onTitleChange = { /* Title update */ },
                onUrlChange = { url ->
                    currentUrl = url
                },
                onRendererCrashed = {
                    browserController = null
                    browserGeneration++
                },
                onLaunchFilePicker = { intent, callback ->
                    pendingFileCallback?.invoke(null)
                    pendingFileCallback = callback
                    try {
                        filePickerLauncher.launch(intent)
                        true
                    } catch (_: Exception) {
                        pendingFileCallback = null
                        false
                    }
                }
            )
        }

        // Hairline loading indicator isolated in its own scope so progress ticks never recompose the browser container.
        TopProgressIndicator(
            progressState = pageProgressState,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(androidx.compose.ui.Alignment.TopCenter)
                .statusBarsPadding()
        )

        val activeColor = remember(activeAccount?.colorHex) {
            try {
                Color(android.graphics.Color.parseColor(activeAccount?.colorHex ?: "#4E73DF"))
            } catch (_: Exception) {
                Color(0xFF4E73DF)
            }
        }

        // Floating Action Pill
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
            onReloadClick = { browserController?.reload() },
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
            currentUrl = currentUrl,
            canGoBack = browserController?.canGoBack() == true,
            canGoForward = browserController?.canGoForward() == true,
            onGoBack = { browserController?.goBack() },
            onGoForward = { browserController?.goForward() },
            onGoHome = {
                browserController?.loadHome(activeAccount?.url ?: AccountRepository.DEFAULT_REMOTE_URL)
            },
            onUpdateKeepScreenOn = { enabled ->
                settingsRepository.setKeepScreenOn(enabled)
            },
            onOpenInExternalBrowser = {
                val target = activeAccount?.url ?: currentUrl
                val uri = runCatching { Uri.parse(target) }.getOrNull()
                if (uri != null) {
                    val opened = runCatching {
                        CustomTabsIntent.Builder()
                            .setShowTitle(true)
                            .build()
                            .launchUrl(context, uri)
                        true
                    }.getOrElse {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            true
                        }.getOrDefault(false)
                    }
                    if (!opened) {
                        Toast.makeText(context, "Không tìm thấy trình duyệt trên thiết bị.", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onClearCurrentAccountData = {
                val id = activeAccount?.id
                if (id != null) {
                    GeckoRuntimeHolder.clearAccountData(id)
                    browserGeneration++
                }
            },
            onClearCache = {
                GeckoRuntimeHolder.clearCache()
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
                    GeckoRuntimeHolder.clearAccountData(accountToEdit!!.id)
                    accountRepository.deleteAccount(accountToEdit!!.id)
                }
                showAccountDialog = false
            }
        )
    }
}

@Composable
private fun TopProgressIndicator(
    progressState: IntState,
    modifier: Modifier = Modifier
) {
    val progress = progressState.intValue
    val animatedProgress by animateFloatAsState(
        targetValue = (progress.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = AppMotion.Fast, easing = AppMotion.AppleEaseOut),
        label = "topProgress"
    )
    AnimatedVisibility(
        visible = progress in 1..99,
        enter = fadeIn(tween(AppMotion.Micro, easing = AppMotion.AppleSilk)),
        exit = fadeOut(tween(AppMotion.Fast, easing = AppMotion.AppleEase)),
        modifier = modifier
    ) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent
        )
    }
}
