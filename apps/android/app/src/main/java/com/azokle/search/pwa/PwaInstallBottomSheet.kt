package com.azokle.search.pwa

import android.widget.Toast
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import kotlinx.coroutines.launch

/**
 * Ultra-Minimalist Add to Home Screen & PWA Install Sheet:
 *  - Crisp single-line header with compact close button
 *  - Flat elevated preview card with 36dp favicon/logo & origin lock indicator
 *  - Streamlined single-line name input
 *  - Responsive primary action button with tactile haptic feedback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PwaInstallBottomSheet(
    eligibility: PwaInstallEligibility,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val colors = AzokleTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isInstalling by remember { mutableStateOf(false) }

    var appTitle by remember { mutableStateOf(eligibility.title) }
    val isPwa = eligibility.isInstallablePwa
    val headerTitle = if (isPwa) "Install App" else "Add to Home Screen"
    val actionButtonText = if (isPwa) "Install" else "Add"

    val host = remember(eligibility.verifiedOrigin) {
        try {
            android.net.Uri.parse(eligibility.verifiedOrigin).host?.removePrefix("www.")
                ?: eligibility.verifiedOrigin.removePrefix("https://").removePrefix("http://").removePrefix("www.")
        } catch (e: Exception) {
            eligibility.verifiedOrigin
        }
    }

    val submitAction: () -> Unit = {
        if (!isInstalling) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            keyboardController?.hide()
            isInstalling = true
            scope.launch {
                val success = PwaShortcutManager.pinPwaShortcut(
                    context = context,
                    eligibility = eligibility,
                    customTitle = appTitle
                )
                isInstalling = false
                if (success) {
                    Toast.makeText(context, "Added '$appTitle' to Home Screen", Toast.LENGTH_SHORT).show()
                    onDismiss()
                } else {
                    Toast.makeText(context, "Failed to create home screen shortcut", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBg,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.borderSubtle)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(560.dp)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = headerTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { heading() }
                )

                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 18.dp, color = colors.textSecondary),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                            }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Close sheet"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .border(0.8.dp, colors.borderSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Preview Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.lg)
                    .background(colors.surfaceElevated)
                    .border(0.8.dp, colors.borderSubtle, AzokleRadius.lg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App / Favicon Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(AzokleRadius.md)
                        .background(colors.cardBg)
                        .border(0.8.dp, colors.borderSubtle, AzokleRadius.md),
                    contentAlignment = Alignment.Center
                ) {
                    if (!eligibility.iconUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(eligibility.iconUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = eligibility.title,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(AzokleRadius.sm),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            text = eligibility.title.take(1).uppercase().ifBlank { "W" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = eligibility.title.ifBlank { "Web Page" },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(1.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (eligibility.isHttps) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = colors.statusSuccess,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = host.ifBlank { eligibility.verifiedOrigin },
                            fontSize = 11.5.sp,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (isPwa) {
                    Box(
                        modifier = Modifier
                            .clip(AzokleRadius.sm)
                            .background(colors.statusSuccess.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PWA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.statusSuccess
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Editable Name Input
            OutlinedTextField(
                value = appTitle,
                onValueChange = { appTitle = it },
                placeholder = {
                    Text(
                        text = "Shortcut name",
                        fontSize = 13.5.sp,
                        color = colors.textMuted
                    )
                },
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submitAction() }),
                modifier = Modifier.fillMaxWidth(),
                shape = AzokleRadius.md,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.actionPrimaryBg,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedContainerColor = colors.surfaceElevated,
                    unfocusedContainerColor = colors.surfaceElevated,
                    cursorColor = colors.actionPrimaryBg
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = AzokleRadius.md
                ) {
                    Text(
                        text = "Cancel",
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.5.sp
                    )
                }

                Button(
                    onClick = submitAction,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(44.dp),
                    shape = AzokleRadius.md,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.actionPrimaryBg,
                        contentColor = colors.actionPrimaryText
                    ),
                    enabled = !isInstalling && appTitle.isNotBlank()
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = colors.actionPrimaryText
                        )
                    } else {
                        Icon(
                            imageVector = if (isPwa) Icons.Default.InstallMobile else Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = actionButtonText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}
