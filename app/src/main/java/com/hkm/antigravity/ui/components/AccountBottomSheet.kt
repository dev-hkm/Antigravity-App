package com.hkm.antigravity.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hkm.antigravity.data.model.Account
import com.hkm.antigravity.ui.theme.*

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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = BorderDark)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quản lý tài khoản",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Button(
                    onClick = onAddAccountClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("+ Thêm tài khoản", fontSize = 13.sp)
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                items(accounts) { account ->
                    val isActive = account.id == activeAccount?.id
                    val color = try {
                        Color(android.graphics.Color.parseColor(account.colorHex))
                    } catch (e: Exception) {
                        AccentBlue
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActive) SurfaceHoverDark else BgDark)
                            .border(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = if (isActive) color else BorderDark,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onSelectAccount(account)
                                onDismiss()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar Initial
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = account.name.take(2).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.name,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 15.sp
                            )
                            Text(
                                text = account.url,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        if (isActive) {
                            Text(
                                text = "Đang dùng",
                                color = color,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }

                        TextButton(
                            onClick = { onEditAccountClick(account) },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Sửa", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
