package com.hkm.antigravity.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.antigravity.data.model.AppSettings
import com.hkm.antigravity.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settings: AppSettings,
    onDismiss: () -> Unit,
    onUpdateAutoConnect: (Boolean) -> Unit,
    onUpdateThemeMode: (String) -> Unit,
    onUpdateDefaultZenMode: (Boolean) -> Unit,
    onClearCache: () -> Unit
) {
    val context = LocalContext.current
    val isDark = when (settings.themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val surfaceColor = if (isDark) SurfaceDark else SurfaceLight
    val borderColor = if (isDark) BorderDark else BorderLight
    val textPrimary = if (isDark) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDark) TextSecondaryDark else TextSecondaryLight
    val itemBg = if (isDark) SurfaceElevatedDark else SurfaceElevatedLight

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = borderColor)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = AccentBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Configure your Antigravity experience",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }

            // Section: Remote Session
            SettingsSectionHeader(title = "REMOTE CONNECTION", textColor = textSecondary)
            
            SettingsSwitchItem(
                icon = Icons.Default.Bolt,
                title = "Auto-Connect",
                subtitle = "Automatically click Connect on instances screen",
                checked = settings.autoConnect,
                onCheckedChange = onUpdateAutoConnect,
                surfaceColor = itemBg,
                borderColor = borderColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            SettingsSwitchItem(
                icon = Icons.Default.Fullscreen,
                title = "Start in Fullscreen",
                subtitle = "Hide top header by default for max screen space",
                checked = settings.defaultZenMode,
                onCheckedChange = onUpdateDefaultZenMode,
                surfaceColor = itemBg,
                borderColor = borderColor,
                textPrimary = textPrimary,
                textSecondary = textSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Appearance / Theme
            SettingsSectionHeader(title = "APPEARANCE", textColor = textSecondary)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(itemBg)
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ThemeOptionButton(
                    modifier = Modifier.weight(1f),
                    title = "System",
                    icon = Icons.Default.BrightnessAuto,
                    isSelected = settings.themeMode == "system",
                    isDark = isDark,
                    onClick = { onUpdateThemeMode("system") }
                )
                ThemeOptionButton(
                    modifier = Modifier.weight(1f),
                    title = "Dark",
                    icon = Icons.Default.DarkMode,
                    isSelected = settings.themeMode == "dark",
                    isDark = isDark,
                    onClick = { onUpdateThemeMode("dark") }
                )
                ThemeOptionButton(
                    modifier = Modifier.weight(1f),
                    title = "Light",
                    icon = Icons.Default.LightMode,
                    isSelected = settings.themeMode == "light",
                    isDark = isDark,
                    onClick = { onUpdateThemeMode("light") }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Data & Storage
            SettingsSectionHeader(title = "DATA & STORAGE", textColor = textSecondary)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(itemBg)
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .clickable {
                        onClearCache()
                        Toast.makeText(context, "WebView cache cleared", Toast.LENGTH_SHORT).show()
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear Cache",
                    tint = DangerRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Clear WebView Cache",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                    Text(
                        text = "Free up storage without logging out accounts",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: About
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Antigravity Remote Client",
                    fontSize = 12.sp,
                    color = textSecondary
                )
                Text(
                    text = "v1.1.0 • Signed Release",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccentBlue
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, textColor: Color) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = textColor,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
private fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    surfaceColor: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = AccentBlue,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = textSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentBlue
            )
        )
    }
}

@Composable
private fun ThemeOptionButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val selectedBg = if (isDark) SurfaceDark else Color.White
    val contentColor = if (isSelected) AccentBlue else if (isDark) TextSecondaryDark else TextSecondaryLight

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) selectedBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}
