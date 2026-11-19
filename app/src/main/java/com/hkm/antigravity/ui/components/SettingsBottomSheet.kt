package com.hkm.antigravity.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.antigravity.data.model.AppSettings
import com.hkm.antigravity.ui.theme.AppMotion
import com.hkm.antigravity.ui.theme.Lucide
import com.hkm.antigravity.ui.theme.appleEntrance
import com.hkm.antigravity.ui.theme.appleScaleOnPress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onUpdateAutoConnect: (Boolean) -> Unit,
    onUpdateThemeMode: (String) -> Unit,
    onUpdateWebTheme: (String) -> Unit,
    onUpdateDynamicColor: (Boolean) -> Unit,
    onUpdateDesktopMode: (Boolean) -> Unit,
    onUpdatePillOrientation: (String) -> Unit,
    onUpdatePillScale: (String) -> Unit,
    onUpdatePillOpacity: (Float) -> Unit,
    onUpdateToolbarButtons: (showAccount: Boolean, showDesktop: Boolean, showWebTheme: Boolean, showReload: Boolean, showSettings: Boolean) -> Unit,
    currentUrl: String = "",
    canGoBack: Boolean = false,
    canGoForward: Boolean = false,
    onGoBack: () -> Unit = {},
    onGoForward: () -> Unit = {},
    onGoHome: () -> Unit = {},
    onUpdateKeepScreenOn: (Boolean) -> Unit = {},
    onUpdateNotifications: (Boolean) -> Unit = {},
    onOpenInExternalBrowser: () -> Unit = {},
    onClearCurrentAccountData: () -> Unit = {},
    onClearCache: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val appVersion = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 4.dp,
                bottom = 32.dp + navBottom
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Settings",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Customize your Antigravity companion experience",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item(key = "web_theme") {
                SettingsSectionHeader(title = "WEB THEME (ANTIGRAVITY WEB)")
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Web Interface Mode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Changes the live theme of Antigravity web directly",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val webOptions = listOf("dark" to "Dark", "light" to "Light", "follow_app" to "Follow App")
                            webOptions.forEachIndexed { index, (key, label) ->
                                SegmentedButton(
                                    selected = settings.webTheme == key,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdateWebTheme(key)
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = webOptions.size)
                                ) {
                                    Text(label, fontSize = 12.sp, fontWeight = if (settings.webTheme == key) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            }

            item(key = "app_theme") {
                Spacer(modifier = Modifier.height(6.dp))
                SettingsSectionHeader(title = "APP THEME & APPEARANCE")
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            SettingsRowSwitch(
                                icon = Lucide.Sparkles,
                                title = "Dynamic Color (Material You)",
                                subtitle = "Harmonize app colors with device wallpaper",
                                checked = settings.dynamicColor,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onUpdateDynamicColor(it)
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }

                        Text(
                            text = "App Shell Theme",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Theme for dialogs, menus and toolbars",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val appOptions = listOf("system" to "System", "dark" to "Dark", "light" to "Light")
                            appOptions.forEachIndexed { index, (key, label) ->
                                SegmentedButton(
                                    selected = settings.themeMode == key,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdateThemeMode(key)
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = appOptions.size)
                                ) {
                                    Text(label, fontSize = 12.sp, fontWeight = if (settings.themeMode == key) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            }

            item(key = "toolbar_custom") {
                Spacer(modifier = Modifier.height(6.dp))
                SettingsSectionHeader(title = "TOOLBAR PILL CUSTOMIZATION")
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Pill Orientation",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val orientations = listOf("horizontal" to "Horizontal", "vertical" to "Vertical")
                            orientations.forEachIndexed { index, (key, label) ->
                                SegmentedButton(
                                    selected = settings.pillOrientation == key,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdatePillOrientation(key)
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = orientations.size)
                                ) {
                                    Text(label, fontSize = 12.sp, fontWeight = if (settings.pillOrientation == key) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Pill Size / Scale",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val scales = listOf("compact" to "Compact", "standard" to "Standard", "large" to "Large")
                            scales.forEachIndexed { index, (key, label) ->
                                SegmentedButton(
                                    selected = settings.pillScale == key,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onUpdatePillScale(key)
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = scales.size)
                                ) {
                                    Text(label, fontSize = 12.sp, fontWeight = if (settings.pillScale == key) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Toolbar Opacity",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Adjust how strongly the floating toolbar stands out",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Slider(
                            value = settings.pillOpacity,
                            onValueChange = onUpdatePillOpacity,
                            valueRange = 0.35f..1f,
                            steps = 12,
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Text(
                            text = "Visible Toolbar Actions",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        PillActionToggleRow("Account Switcher", settings.showAccountOnPill) {
                            onUpdateToolbarButtons(it, settings.showDesktopOnPill, settings.showWebThemeOnPill, settings.showReloadOnPill, settings.showSettingsOnPill)
                        }
                        PillActionToggleRow("Desktop Mode Toggle", settings.showDesktopOnPill) {
                            onUpdateToolbarButtons(settings.showAccountOnPill, it, settings.showWebThemeOnPill, settings.showReloadOnPill, settings.showSettingsOnPill)
                        }
                        PillActionToggleRow("Web Theme Switcher", settings.showWebThemeOnPill) {
                            onUpdateToolbarButtons(settings.showAccountOnPill, settings.showDesktopOnPill, it, settings.showReloadOnPill, settings.showSettingsOnPill)
                        }
                        PillActionToggleRow("Reload Button", settings.showReloadOnPill) {
                            onUpdateToolbarButtons(settings.showAccountOnPill, settings.showDesktopOnPill, settings.showWebThemeOnPill, it, settings.showSettingsOnPill)
                        }
                        PillActionToggleRow("Settings Button", settings.showSettingsOnPill) {
                            onUpdateToolbarButtons(settings.showAccountOnPill, settings.showDesktopOnPill, settings.showWebThemeOnPill, settings.showReloadOnPill, it)
                        }
                    }
                }
            }

            item(key = "quick_nav") {
                SettingsSectionHeader(title = "SESSION & NAVIGATION")
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleEntrance(index = 0),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (currentUrl.isNotBlank()) {
                            Text(
                                text = currentUrl,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickNavButton(
                                icon = Lucide.ArrowBack,
                                label = "Back",
                                enabled = canGoBack,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    onGoBack()
                                }
                            )
                            QuickNavButton(
                                icon = Lucide.ArrowForward,
                                label = "Forward",
                                enabled = canGoForward,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    onGoForward()
                                }
                            )
                            QuickNavButton(
                                icon = Lucide.Home,
                                label = "Home",
                                enabled = true,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    onGoHome()
                                }
                            )
                            QuickNavButton(
                                icon = Lucide.Copy,
                                label = "Copy URL",
                                enabled = currentUrl.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Antigravity URL", currentUrl))
                                    Toast.makeText(context, "URL copied", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            item(key = "remote_conn") {
                Spacer(modifier = Modifier.height(6.dp))
                SettingsSectionHeader(title = "REMOTE CONNECTION")
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleEntrance(index = 1),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingsRowSwitch(
                            icon = Lucide.Bolt,
                            title = "Auto-Connect",
                            subtitle = "Automatically click Connect on instance list",
                            checked = settings.autoConnect,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateAutoConnect(it)
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingsRowSwitch(
                            icon = Lucide.Monitor,
                            title = "Desktop View Mode",
                            subtitle = "Wide layout for reviewing code diffs and terminal",
                            checked = settings.isDesktopMode,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateDesktopMode(it)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingsRowSwitch(
                            icon = Lucide.KeepAwake,
                            title = "Keep Screen On",
                            subtitle = "Prevent sleep while monitoring active agent runs",
                            checked = settings.keepScreenOn,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateKeepScreenOn(it)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        SettingsRowSwitch(
                            icon = Lucide.Notifications,
                            title = "Agent Notifications",
                            subtitle = "Allow Antigravity to send Android system notifications",
                            checked = settings.enableNotifications,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onUpdateNotifications(it)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .appleScaleOnPress(pressedScale = 0.975f)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    onOpenInExternalBrowser()
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Lucide.OpenInBrowser,
                                contentDescription = "Open in Browser",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Open in External Browser",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Launch the current page in your device default browser",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item(key = "data_storage") {
                Spacer(modifier = Modifier.height(6.dp))
                SettingsSectionHeader(title = "DATA & STORAGE")
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleEntrance(index = 2),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .appleScaleOnPress(pressedScale = 0.975f)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onClearCache()
                                    Toast.makeText(context, "Browser cache cleared", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Lucide.Trash,
                                contentDescription = "Clear Cache",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Clear Browser Cache",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Free up memory without logging out accounts",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .appleScaleOnPress(pressedScale = 0.975f)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    onClearCurrentAccountData()
                                    Toast.makeText(context, "Current account session cleared", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Lucide.ClearAccount,
                                contentDescription = "Clear Account Session",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reset Current Account Session",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Sign out and clear cookies for this profile only",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item(key = "footer") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Antigravity Remote Client",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "v$appVersion • Signed Release",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsRowSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun PillActionToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun QuickNavButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .appleScaleOnPress(enabled = enabled, pressedScale = 0.93f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (enabled) 0.8f else 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                }
            )
        }
    }
}
