package com.hkm.antigravity.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.antigravity.data.model.Account
import com.hkm.antigravity.data.repository.AccountRepository
import com.hkm.antigravity.ui.theme.*

private val colorOptions = listOf(
    "#3b82f6", "#6366f1", "#8b5cf6", "#ec4899",
    "#f43f5e", "#f59e0b", "#10b981", "#06b6d4"
)

@Composable
fun AccountDialog(
    accountToEdit: Account? = null,
    canDelete: Boolean = false,
    isDarkTheme: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String, url: String) -> Unit,
    onDelete: () -> Unit = {}
) {
    var name by remember { mutableStateOf(accountToEdit?.name ?: "") }
    var url by remember { mutableStateOf(accountToEdit?.url ?: AccountRepository.DEFAULT_REMOTE_URL) }
    var selectedColor by remember { mutableStateOf(accountToEdit?.colorHex ?: "#3b82f6") }

    val surfaceColor = if (isDarkTheme) SurfaceDark else SurfaceLight
    val borderColor = if (isDarkTheme) BorderDark else BorderLight
    val textPrimary = if (isDarkTheme) TextPrimaryDark else TextPrimaryLight
    val textSecondary = if (isDarkTheme) TextSecondaryDark else TextSecondaryLight

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = if (accountToEdit != null) "Edit Account" else "Add New Account",
                color = textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name (e.g. Work, Personal)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = borderColor,
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = textSecondary
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
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = borderColor,
                        focusedLabelColor = AccentBlue,
                        unfocusedLabelColor = textSecondary
                    )
                )

                Text(
                    text = "Profile Accent Color",
                    color = textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colorOptions.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) (if (isDarkTheme) Color.White else Color.Black) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, selectedColor, url)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (accountToEdit != null && canDelete) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                    ) {
                        Text("Delete")
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = textSecondary)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}
