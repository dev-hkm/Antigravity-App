package com.hkm.antigravity.ui.components

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.antigravity.data.model.AppSettings
import com.hkm.antigravity.ui.theme.Lucide

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
    onOpenInExternalBrowser: () -> Unit = {},
    onClearCache: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val appVersion = androidx.compose.runtime.remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
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

            // Section 1: WEB THEME (ANTIGRAVITY WEB)
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

            Spacer(modifier = Modifier.height(18.dp))

            // Section 2: APP THEME & DYNAMIC COLOR
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

            Spacer(modifier = Modifier.height(18.dp))

            // Section 3: TOOLBAR CUSTOMIZATION
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

            Spacer(modifier = Modifier.height(18.dp))

            // Section 4: REMOTE ENGINE
            SettingsSectionHeader(title = "REMOTE CONNECTION")

            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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
                            contentDescription = "Open in Chrome",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Open in Chrome (Secure Browser)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Use Chrome Custom Tab if Google Sign-In blocks WebView",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section 5: DATA & STORAGE
            SettingsSectionHeader(title = "DATA & STORAGE")

            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClearCache()
                        Toast.makeText(context, "WebView cache cleared", Toast.LENGTH_SHORT).show()
                    },
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
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
                            text = "Clear WebView Cache",
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
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Version info
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
