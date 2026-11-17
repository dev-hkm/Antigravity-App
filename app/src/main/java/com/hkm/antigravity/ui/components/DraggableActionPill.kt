package com.hkm.antigravity.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hkm.antigravity.ui.theme.AppMotion
import com.hkm.antigravity.ui.theme.Lucide
import com.hkm.antigravity.ui.theme.appleScaleOnPress
import com.hkm.antigravity.ui.theme.rememberReduceMotion
import kotlinx.coroutines.launch

@Composable
fun DraggableActionPill(
    modifier: Modifier = Modifier,
    accountColor: Color,
    isDesktopMode: Boolean,
    webTheme: String,
    orientation: String = "horizontal",
    scale: String = "standard",
    opacity: Float = 0.90f,
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
    val scope = rememberCoroutineScope()
    val reduceMotion = rememberReduceMotion()

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val (iconSize, btnSize, padH, padV) = when (scale) {
        "compact" -> listOf(15.dp, 26.dp, 6.dp, 4.dp)
        "large" -> listOf(20.dp, 36.dp, 10.dp, 6.dp)
        else -> listOf(18.dp, 30.dp, 8.dp, 5.dp)
    }

    val isVertical = orientation == "vertical"

    var measuredWidthPx by remember { mutableIntStateOf(0) }
    var measuredHeightPx by remember { mutableIntStateOf(0) }
    val estimatedWidthPx = with(density) { (if (isVertical) 52.dp else 190.dp).toPx() }
    val estimatedHeightPx = with(density) { (if (isVertical) 190.dp else 44.dp).toPx() }
    val pillW = if (measuredWidthPx > 0) measuredWidthPx.toFloat() else estimatedWidthPx
    val pillH = if (measuredHeightPx > 0) measuredHeightPx.toFloat() else estimatedHeightPx
    val safeMargin = with(density) { 6.dp.toPx() }
    val minX = safeMargin
    val maxX = (screenWidthPx - pillW - safeMargin).coerceAtLeast(minX)
    val minY = with(density) { 32.dp.toPx() }
    val maxY = (screenHeightPx - pillH - with(density) { 50.dp.toPx() }).coerceAtLeast(minY)

    val currentMinX by rememberUpdatedState(minX)
    val currentMaxX by rememberUpdatedState(maxX)
    val currentMinY by rememberUpdatedState(minY)
    val currentMaxY by rememberUpdatedState(maxY)

    val offsetX = remember { Animatable(maxX) }
    val offsetY = remember { Animatable(with(density) { 70.dp.toPx() }) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(minX, maxX, minY, maxY) {
        if (!isDragging) {
            val clampedX = offsetX.value.coerceIn(minX, maxX)
            val clampedY = offsetY.value.coerceIn(minY, maxY)
            if (clampedX != offsetX.value) {
                launch { offsetX.animateTo(clampedX, AppMotion.snappySpring()) }
            }
            if (clampedY != offsetY.value) {
                launch { offsetY.animateTo(clampedY, AppMotion.snappySpring()) }
            }
        }
    }

    val pillAlpha by animateFloatAsState(
        targetValue = if (isDragging) 1.0f else opacity.coerceIn(0.35f, 1f),
        animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
        label = "pillAlpha"
    )
    val dragScale by animateFloatAsState(
        targetValue = if (isDragging && !reduceMotion) 1.045f else 1.0f,
        animationSpec = if (isDragging) AppMotion.pressDownSpring() else AppMotion.pressReleaseSpring(),
        label = "pillDragScale"
    )
    val elevation by animateDpAsState(
        targetValue = if (isDragging) 14.dp else 6.dp,
        animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleEaseOut),
        label = "pillElevation"
    )

    val reloadSpin = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .onSizeChanged {
                measuredWidthPx = it.width
                measuredHeightPx = it.height
            }
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                scaleX = dragScale
                scaleY = dragScale
                alpha = pillAlpha
            }
            .pointerInput(orientation, scale) {
                val velocityTracker = VelocityTracker()
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        velocityTracker.resetTracking()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        isDragging = false
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val vel = velocityTracker.calculateVelocity()
                        val targetX = if (reduceMotion) {
                            offsetX.value.coerceIn(currentMinX, currentMaxX)
                        } else {
                            (offsetX.value + AppMotion.projectOffset(vel.x, 0.985f))
                                .coerceIn(currentMinX, currentMaxX)
                        }
                        val targetY = if (reduceMotion) {
                            offsetY.value.coerceIn(currentMinY, currentMaxY)
                        } else {
                            (offsetY.value + AppMotion.projectOffset(vel.y, 0.985f))
                                .coerceIn(currentMinY, currentMaxY)
                        }
                        scope.launch {
                            launch {
                                offsetX.animateTo(
                                    targetValue = targetX,
                                    animationSpec = AppMotion.momentumFlickSpring(),
                                    initialVelocity = vel.x
                                )
                            }
                            launch {
                                offsetY.animateTo(
                                    targetValue = targetY,
                                    animationSpec = AppMotion.momentumFlickSpring(),
                                    initialVelocity = vel.y
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val nextX = (offsetX.value + dragAmount.x).coerceIn(currentMinX, currentMaxX)
                        val nextY = (offsetY.value + dragAmount.y).coerceIn(currentMinY, currentMaxY)
                        velocityTracker.addPosition(change.uptimeMillis, Offset(nextX, nextY))
                        scope.launch {
                            offsetX.snapTo(nextX)
                            offsetY.snapTo(nextY)
                        }
                    }
                )
            }
            .shadow(
                elevation = elevation,
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
            .animateContentSize(animationSpec = AppMotion.intSizeLayoutSpring())
            .padding(horizontal = padH, vertical = padV)
    ) {
        val triggerReload: () -> Unit = {
            if (!reduceMotion) {
                scope.launch {
                    reloadSpin.snapTo(0f)
                    reloadSpin.animateTo(
                        targetValue = 360f,
                        animationSpec = tween(durationMillis = 480, easing = AppMotion.AppleSheet)
                    )
                }
            }
            onReloadClick()
        }

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
                    PillIconButton(
                        icon = if (isDesktopMode) Lucide.Monitor else Lucide.Smartphone,
                        btnSize = btnSize,
                        iconSize = iconSize,
                        isActive = isDesktopMode,
                        onClick = onToggleDesktopMode,
                        haptic = haptic
                    )
                }
                if (showWebTheme) {
                    PillIconButton(
                        icon = if (webTheme == "dark") Lucide.Moon else Lucide.Sun,
                        btnSize = btnSize,
                        iconSize = iconSize,
                        isActive = false,
                        onClick = onToggleWebTheme,
                        haptic = haptic
                    )
                }
                if (showReload) {
                    PillIconButton(
                        icon = Lucide.Refresh,
                        btnSize = btnSize,
                        iconSize = iconSize,
                        isActive = false,
                        rotationDegrees = reloadSpin.value,
                        onClick = triggerReload,
                        haptic = haptic
                    )
                }
                if (showSettings) {
                    PillIconButton(
                        icon = Lucide.Settings,
                        btnSize = btnSize,
                        iconSize = iconSize,
                        isActive = false,
                        onClick = onSettingsClick,
                        haptic = haptic
                    )
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
                    PillIconButton(
                        icon = if (isDesktopMode) Lucide.Monitor else Lucide.Smartphone,
                        btnSize = btnSize,
                        iconSize = iconSize,
                        isActive = isDesktopMode,
                        onClick = onToggleDesktopMode,
                        haptic = haptic
                    )
                }
                if (showWebTheme) {
                    PillIconButton(
                        icon = if (webTheme == "dark") Lucide.Moon else Lucide.Sun,
                        btnSize = btnSize,
                        iconSize = iconSize,
                        isActive = false,
                        onClick = onToggleWebTheme,
                        haptic = haptic
                    )
                }
                if (showReload) {
                    PillIconButton(
                        icon = Lucide.Refresh,
                        btnSize = btnSize,
                        iconSize = iconSize,
                        isActive = false,
                        rotationDegrees = reloadSpin.value,
                        onClick = triggerReload,
                        haptic = haptic
                    )
                }
                if (showSettings) {
                    PillIconButton(
                        icon = Lucide.Settings,
                        btnSize = btnSize,
                        iconSize = iconSize,
                        isActive = false,
                        onClick = onSettingsClick,
                        haptic = haptic
                    )
                }
            }
        }
    }
}

@Composable
private fun PillAccountButton(
    accountColor: Color,
    btnSize: Dp,
    onClick: () -> Unit,
    haptic: HapticFeedback
) {
    val animatedColor by animateColorAsState(
        targetValue = accountColor,
        animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
        label = "pillAccountColor"
    )
    Box(
        modifier = Modifier
            .size(btnSize)
            .appleScaleOnPress(pressedScale = 0.88f)
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
                .size(16.dp)
                .clip(CircleShape)
                .background(animatedColor.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(animatedColor)
            )
        }
    }
}

@Composable
private fun PillIconButton(
    icon: ImageVector,
    btnSize: Dp,
    iconSize: Dp,
    isActive: Boolean,
    rotationDegrees: Float = 0f,
    onClick: () -> Unit,
    haptic: HapticFeedback
) {
    val iconTint by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
        label = "pillIconTint"
    )
    IconButton(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier
            .size(btnSize)
            .appleScaleOnPress(pressedScale = 0.88f)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer { rotationZ = rotationDegrees }
        )
    }
}
