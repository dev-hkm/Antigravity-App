package com.hkm.antigravity.ui.screens

import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.antigravity.data.model.Account
import com.hkm.antigravity.data.repository.AccountRepository
import com.hkm.antigravity.ui.components.AccountBottomSheet
import com.hkm.antigravity.ui.components.AccountDialog
import com.hkm.antigravity.ui.components.WebViewContainer
import com.hkm.antigravity.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: AccountRepository,
    onFinish: () -> Unit
) {
    val accounts by repository.accountsFlow.collectAsState()
    val activeAccount by repository.activeAccountFlow.collectAsState()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf(activeAccount?.url ?: "https://antigravity.google") }
    var pageProgress by remember { mutableIntStateOf(0) }
    var isZenMode by remember { mutableStateOf(false) }

    var showAccountSheet by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<Account?>(null) }

    // Intercept back button to navigate webview history
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BgDark,
        topBar = {
            if (!isZenMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgSidebar)
                        .statusBarsPadding()
                ) {
                    // Header Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brand Icon & Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                webViewInstance?.loadUrl(activeAccount?.url ?: "https://antigravity.google")
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(AccentBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", fontWeight = FontWeight.Bold, color = AccentBlue, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Antigravity",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Active Account Pill (Tap to switch accounts)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(SurfaceDark)
                                .border(1.dp, activeColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .clickable { showAccountSheet = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(activeColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = activeAccount?.name ?: "Accounts",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Switch Account",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Zen Mode Toggle (Expand to fullscreen)
                        IconButton(
                            onClick = { isZenMode = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Zen Mode",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Navigation Bar (Back, Forward, Refresh, URL display)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { webViewInstance?.goBack() },
                            enabled = webViewInstance?.canGoBack() == true,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (webViewInstance?.canGoBack() == true) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { webViewInstance?.goForward() },
                            enabled = webViewInstance?.canGoForward() == true,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Forward",
                                tint = if (webViewInstance?.canGoForward() == true) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                activeAccount?.let { repository.saveCurrentAccountCookies(currentUrl) }
                                webViewInstance?.reload()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = TextPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Compact URL / Title Display
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(30.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceDark)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "SSL",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentUrl,
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Progress bar
                    if (pageProgress in 1..99) {
                        LinearProgressIndicator(
                            progress = { pageProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = AccentBlue,
                            trackColor = Color.Transparent
                        )
                    } else {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgDark)
        ) {
            // Main Web View
            WebViewContainer(
                modifier = Modifier.fillMaxSize(),
                url = activeAccount?.url ?: "https://antigravity.google",
                onWebViewCreated = { webView ->
                    webViewInstance = webView
                    activeAccount?.let { acc ->
                        repository.applyAccountCookies(acc, acc.url)
                    }
                },
                onProgressChange = { pageProgress = it },
                onTitleChange = { /* Title update */ },
                onUrlChange = { url ->
                    currentUrl = url
                    activeAccount?.let { repository.saveCurrentAccountCookies(url) }
                }
            )

            // Floating button to exit Zen Mode
            AnimatedVisibility(
                visible = isZenMode,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = { isZenMode = false },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark.copy(alpha = 0.85f))
                ) {
                    Icon(
                        imageVector = Icons.Default.FullscreenExit,
                        contentDescription = "Exit Zen Mode",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    // Account Switcher Bottom Sheet
    if (showAccountSheet) {
        AccountBottomSheet(
            accounts = accounts,
            activeAccount = activeAccount,
            onDismiss = { showAccountSheet = false },
            onSelectAccount = { target ->
                val newAcc = repository.switchAccount(target.id, currentUrl)
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

    // Account Add / Edit Dialog
    if (showAccountDialog) {
        AccountDialog(
            accountToEdit = accountToEdit,
            canDelete = accounts.size > 1,
            onDismiss = { showAccountDialog = false },
            onSave = { name, colorHex, url ->
                if (accountToEdit != null) {
                    repository.updateAccount(accountToEdit!!.id, name, colorHex, url)
                } else {
                    val created = repository.createAccount(name, colorHex, url)
                    webViewInstance?.loadUrl(created.url)
                }
                showAccountDialog = false
            },
            onDelete = {
                if (accountToEdit != null) {
                    repository.deleteAccount(accountToEdit!!.id)
                    repository.getActiveAccount()?.let { next ->
                        webViewInstance?.loadUrl(next.url)
                    }
                }
                showAccountDialog = false
            }
        )
    }
}
