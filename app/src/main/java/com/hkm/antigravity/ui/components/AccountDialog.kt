package com.hkm.antigravity.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.antigravity.data.model.Account
import com.hkm.antigravity.data.repository.AccountRepository
import com.hkm.antigravity.ui.theme.AppMotion
import com.hkm.antigravity.ui.theme.appleScaleOnPress
import java.util.Locale

private val curatedColorSwatches = listOf(
    "#3B82F6", "#6366F1", "#8B5CF6", "#EC4899", "#F43F5E", "#EF4444",
    "#F97316", "#F59E0B", "#10B981", "#14B8A6", "#06B6D4", "#64748B"
)

private fun parseColorOrDefault(hex: String, fallback: Int = android.graphics.Color.parseColor("#3B82F6")): Int {
    return try {
        android.graphics.Color.parseColor(hex)
    } catch (_: Exception) {
        fallback
    }
}

private fun argbToHex(argb: Int): String =
    String.format(Locale.US, "#%06X", 0xFFFFFF and argb)

private fun argbToHsv(argb: Int): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    return hsv
}

@Composable
fun AccountDialog(
    accountToEdit: Account? = null,
    canDelete: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String, url: String) -> Unit,
    onDelete: () -> Unit = {}
) {
    var name by remember { mutableStateOf(accountToEdit?.name ?: "") }
    var url by remember { mutableStateOf(accountToEdit?.url ?: AccountRepository.DEFAULT_REMOTE_URL) }

    val initialArgb = remember(accountToEdit?.colorHex) {
        parseColorOrDefault(accountToEdit?.colorHex ?: "#3B82F6")
    }
    val initialHsv = remember(initialArgb) { argbToHsv(initialArgb) }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var hexInput by remember { mutableStateOf(argbToHex(initialArgb)) }

    val currentColor = remember(hue, saturation, value) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    }
    val currentHex = remember(currentColor) { argbToHex(currentColor.toArgb()) }

    fun applyHex(hex: String) {
        val parsed = parseColorOrDefault(hex, -1)
        if (parsed != -1) {
            val hsv = argbToHsv(parsed)
            hue = hsv[0]
            saturation = hsv[1]
            value = hsv[2]
            hexInput = argbToHex(parsed)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = if (accountToEdit != null) "Edit Account" else "Add New Account",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live Avatar & Accent Preview
                val initials = name.trim()
                    .split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.take(1).uppercase(Locale.US) }
                    .ifBlank { "AG" }
                val avatarTextColor by animateColorAsState(
                    targetValue = if (currentColor.luminance() > 0.55f) Color(0xFF0F172A) else Color.White,
                    animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleEaseOut),
                    label = "avatarTextColor"
                )
                val animatedPreviewColor by animateColorAsState(
                    targetValue = currentColor,
                    animationSpec = tween(AppMotion.Micro, easing = AppMotion.AppleEaseOut),
                    label = "avatarPreviewColor"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .border(
                            width = 1.dp,
                            color = animatedPreviewColor.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(animatedPreviewColor),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = initials,
                            transitionSpec = { AppMotion.softCrossDissolve() },
                            label = "avatarInitials"
                        ) { text ->
                            Text(
                                text = text,
                                color = avatarTextColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name.trim().ifBlank { "Account Preview" },
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = currentHex,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name (e.g. Work, Personal)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Antigravity URL") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Text(
                    text = "Profile Accent Color",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                // Interactive 2D Saturation × Value Spectrum Pad
                SaturationValuePad(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onChange = { s, v ->
                        saturation = s
                        value = v
                        hexInput = argbToHex(
                            android.graphics.Color.HSVToColor(floatArrayOf(hue, s, v))
                        )
                    }
                )

                // Interactive Rainbow Hue Slider
                HueSliderBar(
                    hue = hue,
                    onHueChange = { newHue ->
                        hue = newHue
                        hexInput = argbToHex(
                            android.graphics.Color.HSVToColor(floatArrayOf(newHue, saturation, value))
                        )
                    }
                )

                // Editable Hex Input + Current Swatch
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { raw ->
                        val cleaned = raw.uppercase(Locale.US)
                            .filter { it == '#' || it in '0'..'9' || it in 'A'..'F' }
                            .take(7)
                        val normalized = if (cleaned.startsWith("#")) cleaned else "#${cleaned.take(6)}"
                        hexInput = normalized
                        if (normalized.length == 7) {
                            val parsed = parseColorOrDefault(normalized, -1)
                            if (parsed != -1) {
                                val hsv = argbToHsv(parsed)
                                hue = hsv[0]
                                saturation = hsv[1]
                                value = hsv[2]
                            }
                        }
                    },
                    label = { Text("Hex Color") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(currentColor)
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                // Curated Quick Swatches (2 rows of 6)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    curatedColorSwatches.chunked(6).forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowColors.forEach { hex ->
                                val swatchColor = Color(parseColorOrDefault(hex))
                                val isSelected = currentHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .appleScaleOnPress(pressedScale = 0.85f)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { applyHex(hex) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, currentHex, url) },
                modifier = Modifier.appleScaleOnPress(pressedScale = 0.94f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (accountToEdit != null && canDelete) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun SaturationValuePad(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (saturation: Float, value: Float) -> Unit
) {
    val latestOnChange by rememberUpdatedState(onChange)
    val hueColor = remember(hue) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val s = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val v = 1f - (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                    latestOnChange(s, v)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val s = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                    latestOnChange(s, v)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, hueColor)
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black)
                )
            )

            val cx = (saturation * size.width).coerceIn(0f, size.width)
            val cy = ((1f - value) * size.height).coerceIn(0f, size.height)
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = 9.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color.White,
                radius = 7.dp.toPx(),
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun HueSliderBar(
    hue: Float,
    onHueChange: (Float) -> Unit
) {
    val latestOnHueChange by rememberUpdatedState(onHueChange)
    val rainbowColors = remember {
        listOf(
            Color(0xFFFF0000),
            Color(0xFFFFFF00),
            Color(0xFF00FF00),
            Color(0xFF00FFFF),
            Color(0xFF0000FF),
            Color(0xFFFF00FF),
            Color(0xFFFF0000)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val h = ((offset.x / size.width.toFloat()).coerceIn(0f, 1f)) * 360f
                    latestOnHueChange(h)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val h = ((change.position.x / size.width.toFloat()).coerceIn(0f, 1f)) * 360f
                    latestOnHueChange(h)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = Brush.horizontalGradient(rainbowColors))
            val tx = ((hue / 360f) * size.width).coerceIn(8.dp.toPx(), size.width - 8.dp.toPx())
            val cy = size.height / 2f
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = 9.dp.toPx(),
                center = Offset(tx, cy)
            )
            drawCircle(
                color = Color.White,
                radius = 7.dp.toPx(),
                center = Offset(tx, cy)
            )
        }
    }
}
