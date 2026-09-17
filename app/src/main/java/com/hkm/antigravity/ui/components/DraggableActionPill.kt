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
    onAccountClick: () -> Unit,
    onToggleDesktopMode: () -> Unit,
    onReloadClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Initial position at upper right corner with safe margins
    var offsetX by remember { mutableFloatStateOf(screenWidthPx - with(density) { 200.dp.toPx() }) }
    var offsetY by remember { mutableFloatStateOf(with(density) { 60.dp.toPx() }) }
    var isDragging by remember { mutableStateOf(false) }

    // Dimming state when idle
    val pillAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1.0f else 0.88f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "pillAlpha"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        isDragging = false
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        // Snap smoothly towards the nearest side if released too close to edge
                        val pillWidth = with(density) { 190.dp.toPx() }
                        val safeMargin = with(density) { 12.dp.toPx() }
                        val minX = safeMargin
                        val maxX = screenWidthPx - pillWidth - safeMargin

                        if (offsetX < screenWidthPx / 2) {
                            offsetX = minX
                        } else {
                            offsetX = maxX
                        }

                        // Clamp Y inside safe bounds
                        val minY = with(density) { 48.dp.toPx() }
                        val maxY = screenHeightPx - with(density) { 120.dp.toPx() }
                        offsetY = offsetY.coerceIn(minY, maxY)
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidthPx - with(density) { 80.dp.toPx() })
                        offsetY = (offsetY + dragAmount.y).coerceIn(with(density) { 30.dp.toPx() }, screenHeightPx - with(density) { 80.dp.toPx() })
                    }
                )
            }
            .alpha(pillAlpha)
            .shadow(
                elevation = if (isDragging) 12.dp else 6.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Drag Grip indicator
            Icon(
                imageVector = Lucide.DragHandle,
                contentDescription = "Drag",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )

            // Account Indicator
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, radius = 14.dp)
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAccountClick()
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

            // Desktop / Mobile View Mode Toggle (Brainstorm Feature #3)
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleDesktopMode()
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = if (isDesktopMode) Lucide.Monitor else Lucide.Smartphone,
                    contentDescription = if (isDesktopMode) "Desktop Mode Active" else "Mobile Mode Active",
                    tint = if (isDesktopMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Reload Page
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onReloadClick()
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Lucide.Refresh,
                    contentDescription = "Reload",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Settings Sheet
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSettingsClick()
                },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Lucide.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
