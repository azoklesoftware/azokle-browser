package com.azokle.search.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth

/**
 * Confirmation Sheet displayed before executing a Fire Incineration / Burn Browsing Data action.
 * Gives user clear details on what will be cleared and allows Confirm or Cancel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurnConfirmationSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBg,
        tonalElevation = 8.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.border)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Fire Icon Badge
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF4500).copy(alpha = 0.15f))
                    .border(1.5.dp, Color(0xFFFF4500).copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = Color(0xFFFF4500),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Title and Description
            Text(
                text = "Burn Browsing Data?",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "This will immediately incinerate your current session data and return you to a clean home page.",
                fontSize = 13.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Summary of Cleared Items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.lg)
                    .background(colors.surfaceElevated)
                    .border(1.dp, colors.border, AzokleRadius.lg)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BurnItemRow(
                    icon = Icons.Default.Layers,
                    iconTint = Color(0xFF3B82F6),
                    text = "Close all open tabs & tab groups"
                )
                BurnItemRow(
                    icon = Icons.Default.Cookie,
                    iconTint = Color(0xFFF59E0B),
                    text = "Clear cookies & website session storage"
                )
                BurnItemRow(
                    icon = Icons.Default.History,
                    iconTint = Color(0xFF8B5CF6),
                    text = "Erase browsing history & web cache"
                )
                BurnItemRow(
                    icon = Icons.Default.DeleteSweep,
                    iconTint = Color(0xFFEF4444),
                    text = "Clear search suggestions & recent queries"
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 4. Action Buttons (Cancel / Burn Data)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel Button
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(AzokleRadius.md)
                        .background(colors.surface)
                        .border(1.dp, colors.border, AzokleRadius.md)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = colors.textPrimary.copy(alpha = 0.1f)),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                            }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Cancel"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }

                // Burn Data Button (Destructive Gradient Button)
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(48.dp)
                        .clip(AzokleRadius.md)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFFFF3B30), Color(0xFFFF6F00))
                            )
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = Color.White.copy(alpha = 0.2f)),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onConfirm()
                            }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Burn Data"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Burn Data",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun BurnItemRow(
    icon: ImageVector,
    iconTint: Color,
    text: String
) {
    val colors = AzokleTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            lineHeight = 16.sp
        )
    }
}
