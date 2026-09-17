package com.hkm.antigravity.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.hkm.antigravity.ui.theme.Lucide
import kotlin.math.roundToInt

@Composable
fun DraggableActionPill(
    modifier: Modifier = Modifier,
    accountColor: Color,
    isDesktopMode: Boolean,
    webTheme: String,
    orientation: String = "horizontal",
    scale: String = "standard",
    showAccount: Boolean = true,
    showDesktop: Boolean = true,
    showWebTheme: Boolean = true,
    showReload: Boolean = true,
    showSettings: Boolean = true,
    onAccountClick: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onToggleWebTheme: () -> Unit,
    onReloadClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val (iconSize, btnSize, padH, padV) = when (scale) {
        "compact" -> listOf(15.dp, 26.dp, 6.dp, 4.dp)
        "large" -> listOf(20.dp, 36.dp, 10.dp, 6.dp)
        else -> listOf(18.dp, 30.dp, 8.dp, 5.dp) // standard
    }

    val isVertical = orientation == "vertical"

    val pillW = with(density) { (if (isVertical) 52.dp else 190.dp).toPx() }
    val pillH = with(density) { (if (isVertical) 190.dp else 44.dp).toPx() }
    val safeMargin = with(density) { 6.dp.toPx() }
    val minX = safeMargin
    val maxX = (screenWidthPx - pillW - safeMargin).coerceAtLeast(minX)
    val minY = with(density) { 32.dp.toPx() }
    val maxY = (screenHeightPx - pillH - with(density) { 50.dp.toPx() }).coerceAtLeast(minY)

    // Initial position
    var offsetX by remember { mutableFloatStateOf(maxX - with(density) { 10.dp.toPx() }) }
    var offsetY by remember { mutableFloatStateOf(with(density) { 70.dp.toPx() }) }
    var isDragging by remember { mutableStateOf(false) }

    val pillAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1.0f else 0.90f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "pillAlpha"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(orientation, scale) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        isDragging = false
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        // Stays EXACTLY where dropped!
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(minX, maxX)
                        offsetY = (offsetY + dragAmount.y).coerceIn(minY, maxY)
                    }
                )
            }
            .alpha(pillAlpha)
            .shadow(
                elevation = if (isDragging) 12.dp else 6.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.4f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = padH, vertical = padV)
    ) {
        if (isVertical) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Lucide.DragHandle,
                    contentDescription = "Drag Handle",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )

                if (showAccount) {
                    PillAccountButton(accountColor, btnSize, onAccountClick, haptic)
                }
                if (showDesktop) {
                    PillIconButton(if (isDesktopMode) Lucide.Monitor else Lucide.Smartphone, btnSize, iconSize, isDesktopMode, onToggleDesktopMode, haptic)
                }
                if (showWebTheme) {
                    PillIconButton(if (webTheme == "dark") Lucide.Moon else Lucide.Sun, btnSize, iconSize, false, onToggleWebTheme, haptic)
                }
                if (showReload) {
                    PillIconButton(Lucide.Refresh, btnSize, iconSize, false, onReloadClick, haptic)
                }
                if (showSettings) {
                    PillIconButton(Lucide.Settings, btnSize, iconSize, false, onSettingsClick, haptic)
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Lucide.DragHandle,
                    contentDescription = "Drag Handle",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )

                if (showAccount) {
                    PillAccountButton(accountColor, btnSize, onAccountClick, haptic)
                }
                if (showDesktop) {
                    PillIconButton(if (isDesktopMode) Lucide.Monitor else Lucide.Smartphone, btnSize, iconSize, isDesktopMode, onToggleDesktopMode, haptic)
                }
                if (showWebTheme) {
                    PillIconButton(if (webTheme == "dark") Lucide.Moon else Lucide.Sun, btnSize, iconSize, false, onToggleWebTheme, haptic)
                }
                if (showReload) {
                    PillIconButton(Lucide.Refresh, btnSize, iconSize, false, onReloadClick, haptic)
                }
                if (showSettings) {
                    PillIconButton(Lucide.Settings, btnSize, iconSize, false, onSettingsClick, haptic)
                }
            }
        }
    }
}

@Composable
private fun PillAccountButton(
    accountColor: Color,
    btnSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    Box(
        modifier = Modifier
            .size(btnSize)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 14.dp)
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(accountColor)
        )
    }
}

@Composable
private fun PillIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    btnSize: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    isActive: Boolean,
    onClick: () -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier.size(btnSize)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(iconSize)
        )
    }
}
