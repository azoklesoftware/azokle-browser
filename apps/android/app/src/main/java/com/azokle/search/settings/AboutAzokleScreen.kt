package com.azokle.search.settings

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.azokle.search.common.components.ToastManager
import com.azokle.search.common.theme.AzokleTheme
import java.util.Calendar

import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Native About Azokle Screen matching the clean, modern Brave-style layout.
 * Displays application version, operating system details, and copyright legal information.
 */
@Composable
fun AboutAzokleScreen(
    onBack: () -> Unit,
    onClose: () -> Unit = onBack,
    onOpenUrl: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AzokleTheme.colors
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var showLegalScreen by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (showLegalScreen) {
            showLegalScreen = false
        } else {
            onBack()
        }
    }

    val appVersion = remember(context) {
        val version = try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pInfo.versionName ?: "2.0.5"
        } catch (_: Exception) {
            "2.0.5"
        }
        "Azokle Browser $version, Mozilla Gecko 154.0"
    }

    val osDetails = remember {
        val osVersion = Build.VERSION.RELEASE
        val buildId = if (Build.DISPLAY.isNotBlank()) Build.DISPLAY else Build.ID
        "Android $osVersion; Build/$buildId"
    }

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val legalInfo = remember(currentYear) {
        "Copyright © $currentYear The Azokle Authors. All rights reserved."
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "About Azokle",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Info Card (Matching Brave aesthetic)
            val cardShape = RoundedCornerShape(16.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(cardShape)
                    .background(colors.cardBg)
                    .border(0.8.dp, colors.border.copy(alpha = 0.6f), cardShape)
            ) {
                // 1. Application Version
                AboutInfoItem(
                    title = "Application version",
                    subtitle = appVersion,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        clipboardManager.setText(AnnotatedString(appVersion))
                        ToastManager.showSuccess("Copied version to clipboard")
                    }
                )

                HorizontalDivider(
                    color = colors.border.copy(alpha = 0.45f),
                    thickness = 0.8.dp
                )

                // 2. Operating System
                AboutInfoItem(
                    title = "Operating system",
                    subtitle = osDetails,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        clipboardManager.setText(AnnotatedString(osDetails))
                        ToastManager.showSuccess("Copied OS details to clipboard")
                    }
                )

                HorizontalDivider(
                    color = colors.border.copy(alpha = 0.45f),
                    thickness = 0.8.dp
                )

                // 3. Legal Information
                AboutInfoItem(
                    title = "Legal information",
                    subtitle = legalInfo,
                    trailingIcon = Icons.Default.ChevronRight,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        showLegalScreen = true
                    }
                )
            }
        }

        // Dedicated Legal Information Screen Overlay
        if (showLegalScreen) {
            LegalInformationScreen(
                onBack = { showLegalScreen = false },
                onClose = onClose,
                onOpenUrl = onOpenUrl
            )
        }
    }
}

@Composable
private fun AboutInfoItem(
    title: String,
    subtitle: String,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    val colors = AzokleTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.15f)),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics {
                role = Role.Button
                contentDescription = "$title: $subtitle"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Normal,
                color = colors.textSecondary
            )
        }

        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
