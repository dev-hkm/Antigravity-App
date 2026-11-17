package com.hkm.antigravity.ui.theme

import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Centralized Apple-grade motion tokens & GPU-composited modifiers.
 * Every curve, spring, and micro-interaction pulls from this single vocabulary.
 */
object AppMotion {
    // Canonical Apple cubic-bezier deceleration curves
    val AppleSheet = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    val AppleSilk = CubicBezierEasing(0.28f, 0.11f, 0.14f, 1f)
    val AppleEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    val AppleEase = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

    // Calibrated duration tokens (ms)
    const val Sheet = 440
    const val Content = 360
    const val Fast = 220
    const val Micro = 160

    // Perceptual springs (C^1 continuous, interruptible)
    fun <T> fluidSpring() = spring<T>(dampingRatio = 0.88f, stiffness = 260f)
    fun <T> layoutSpring() = spring<T>(dampingRatio = 0.88f, stiffness = 270f)
    fun <T> gentleSpring() = spring<T>(dampingRatio = 0.92f, stiffness = 210f)
    fun <T> snappySpring() = spring<T>(dampingRatio = 0.82f, stiffness = 360f)
    fun <T> pressDownSpring() = spring<T>(dampingRatio = 0.86f, stiffness = 420f)
    fun <T> pressReleaseSpring() = spring<T>(dampingRatio = 0.68f, stiffness = 300f)
    fun <T> softPopSpring() = spring<T>(dampingRatio = 0.72f, stiffness = 380f)
    fun <T> momentumFlickSpring() = spring<T>(dampingRatio = 0.82f, stiffness = 340f)

    fun intSizeLayoutSpring() = spring<IntSize>(dampingRatio = 0.88f, stiffness = 270f)

    fun softCrossDissolve(): ContentTransform =
        fadeIn(tween(Fast, easing = AppleSilk)) togetherWith
            fadeOut(tween(Micro, easing = AppleEase))

    val softExpandEnter: EnterTransition =
        expandVertically(animationSpec = tween(320, easing = AppleSheet)) +
            fadeIn(tween(280, easing = AppleSilk))

    val softExpandExit: ExitTransition =
        shrinkVertically(animationSpec = tween(220, easing = AppleSilk)) +
            fadeOut(tween(160, easing = AppleEase))

    val floatingBarEnter: EnterTransition =
        slideInVertically(tween(Sheet, easing = AppleSheet)) { -it / 2 } +
            fadeIn(tween(280, easing = AppleSilk)) +
            scaleIn(tween(Sheet, easing = AppleSheet), initialScale = 0.96f)

    val floatingBarExit: ExitTransition =
        slideOutVertically(tween(260, easing = AppleSilk)) { -it / 2 } +
            fadeOut(tween(180, easing = AppleEase)) +
            scaleOut(tween(260, easing = AppleSilk), targetScale = 0.96f)

    /**
     * WWDC18 momentum projection for crisp snap detents.
     */
    fun projectOffset(velocityPxPerSec: Float, decelerationRate: Float = 0.990f): Float =
        (velocityPxPerSec / 1000f) * decelerationRate / (1f - decelerationRate)
}

@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Instant pointer-down compression + spring rebound on release.
 * Purely GPU-composited via graphicsLayer (zero layout pass).
 */
fun Modifier.appleScaleOnPress(
    enabled: Boolean = true,
    pressedScale: Float = 0.965f
): Modifier = composed {
    val reduceMotion = rememberReduceMotion()
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled && !reduceMotion) pressedScale else 1f,
        animationSpec = if (pressed) AppMotion.pressDownSpring() else AppMotion.pressReleaseSpring(),
        label = "apple_press_scale"
    )

    this
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                pressed = true
                waitForUpOrCancellation()
                pressed = false
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}

/**
 * Staggered wave entrance capped at 5 items so lists feel responsive and unhurried.
 * Animates exclusively inside graphicsLayer.
 */
fun Modifier.appleEntrance(
    index: Int = 0,
    stepDelayMs: Int = 38,
    translationYFromPx: Float = 22f
): Modifier = composed {
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) return@composed this

    var visible by remember { mutableStateOf(false) }
    val clampedDelay = (index.coerceIn(0, 5) * stepDelayMs)

    LaunchedEffect(Unit) {
        if (clampedDelay > 0) delay(clampedDelay.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(AppMotion.Content, easing = AppMotion.AppleSilk),
        label = "entrance_alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else translationYFromPx,
        animationSpec = tween(AppMotion.Content, easing = AppMotion.AppleSheet),
        label = "entrance_ty"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.978f,
        animationSpec = tween(AppMotion.Content, easing = AppMotion.AppleSheet),
        label = "entrance_scale"
    )

    graphicsLayer {
        this.alpha = alpha
        this.translationY = offsetY
        this.scaleX = scale
        this.scaleY = scale
    }
}
