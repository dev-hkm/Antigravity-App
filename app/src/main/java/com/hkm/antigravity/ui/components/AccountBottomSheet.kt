package com.hkm.antigravity.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.antigravity.data.model.Account
import com.hkm.antigravity.ui.theme.AppMotion
import com.hkm.antigravity.ui.theme.Lucide
import com.hkm.antigravity.ui.theme.appleEntrance
import com.hkm.antigravity.ui.theme.appleScaleOnPress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBottomSheet(
    accounts: List<Account>,
    activeAccount: Account?,
    onDismiss: () -> Unit,
    onSelectAccount: (Account) -> Unit,
    onAddAccountClick: () -> Unit,
    onEditAccountClick: (Account) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Accounts",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Switch or manage isolated profiles",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onAddAccountClick,
                    modifier = Modifier.appleScaleOnPress(pressedScale = 0.94f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Lucide.Add,
                            contentDescription = "Add",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Account", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                itemsIndexed(accounts, key = { _, it -> it.id }) { index, account ->
                    val isActive = account.id == activeAccount?.id
                    val fallbackColor = MaterialTheme.colorScheme.primary
                    val color = remember(account.colorHex, fallbackColor) {
                        runCatching { Color(android.graphics.Color.parseColor(account.colorHex)) }
                            .getOrDefault(fallbackColor)
                    }
                    val cardBg by animateColorAsState(
                        targetValue = if (isActive) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
                        label = "accountCardBg"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isActive) color else MaterialTheme.colorScheme.outlineVariant,
                        animationSpec = tween(AppMotion.Fast, easing = AppMotion.AppleSilk),
                        label = "accountBorderColor"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .appleEntrance(index = index)
                            .appleScaleOnPress(pressedScale = 0.975f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardBg)
                            .border(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                onSelectAccount(account)
                                onDismiss()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = account.name.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.name,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp
                            )
                            Text(
                                text = account.url,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        AnimatedVisibility(
                            visible = isActive,
                            enter = fadeIn(tween(AppMotion.Fast, easing = AppMotion.AppleSilk)) +
                                scaleIn(AppMotion.softPopSpring(), initialScale = 0.85f),
                            exit = fadeOut(tween(AppMotion.Micro, easing = AppMotion.AppleEase)) +
                                scaleOut(tween(AppMotion.Micro), targetScale = 0.9f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = "Active",
                                    tint = color,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Active",
                                    color = color,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(
                            onClick = { onEditAccountClick(account) },
                            modifier = Modifier
                                .size(32.dp)
                                .appleScaleOnPress(pressedScale = 0.88f)
                        ) {
                            Icon(
                                imageVector = Lucide.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
