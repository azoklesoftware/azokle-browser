package com.azokle.search.browser.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth

@Composable
fun BrowserErrorScreen(
    error: BrowserErrorType,
    failedUrl: String,
    onRetry: () -> Unit,
    onHome: () -> Unit,
    onBypass: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AzokleTheme.colors
    val scrollState = rememberScrollState()

    // Strict Pure Monochrome Color System (Black BG -> White text/icons, White BG -> Black text/icons)
    val isDark = colors.isDark
    val pageBg = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)
    val cardBg = if (isDark) Color(0xFF0D0D0D) else Color(0xFFFAFAFA)
    val elevatedBg = if (isDark) Color(0xFF141414) else Color(0xFFF4F4F5)
    val borderCol = if (isDark) Color(0xFF242424) else Color(0xFFE4E4E7)
    val borderSubtle = if (isDark) Color(0xFF1A1A1A) else Color(0xFFECECEE)

    val primaryTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val secondaryTextColor = if (isDark) Color(0xFFA1A1AA) else Color(0xFF52525B)
    val mutedTextColor = if (isDark) Color(0xFF71717A) else Color(0xFF71717A)
    val primaryIconColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)

    val btnPrimaryBg = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val btnPrimaryText = if (isDark) Color(0xFF000000) else Color(0xFFFFFFFF)

    val btnSecondaryBg = if (isDark) Color(0xFF141414) else Color(0xFFF4F4F5)
    val btnSecondaryText = if (isDark) Color(0xFFFFFFFF) else Color(0xFF000000)
    val btnSecondaryBorder = if (isDark) Color(0xFF2A2A2A) else Color(0xFFE4E4E7)

    val host = remember(failedUrl) {
        try {
            val parsed = Uri.parse(failedUrl)
            parsed.host ?: failedUrl
        } catch (e: Exception) {
            failedUrl
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBg)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(580.dp)
                .verticalScroll(scrollState)
                .padding(vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Ultra-Clean Hero Emblem (Pure Monochromatic Badge)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(AzokleRadius.xl)
                    .background(elevatedBg)
                    .border(1.dp, borderCol, AzokleRadius.xl),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = error.iconVector,
                    contentDescription = null,
                    tint = primaryIconColor,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. Status Badge (Monospace Category Tag)
            Box(
                modifier = Modifier
                    .clip(AzokleRadius.sm)
                    .background(elevatedBg)
                    .border(1.dp, borderCol, AzokleRadius.sm)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = error.category.label,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    letterSpacing = 0.8.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Error Title
            Text(
                text = error.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )

            if (host.isNotBlank() && !host.equals("about:blank", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = host,
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = mutedTextColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Subtitle Description
            Text(
                text = error.subtitle,
                fontSize = 13.5.sp,
                color = secondaryTextColor,
                textAlign = TextAlign.Center,
                lineHeight = 19.5.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Technical Error Code Pill (Tap to Copy)
            Row(
                modifier = Modifier
                    .clip(AzokleRadius.sm)
                    .background(elevatedBg)
                    .border(1.dp, borderCol, AzokleRadius.sm)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = primaryTextColor.copy(alpha = 0.15f)),
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Error Code", error.errorCode))
                            Toast.makeText(context, "Copied: ${error.errorCode}", Toast.LENGTH_SHORT).show()
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Error code: ${error.errorCode}, tap to copy"
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = error.errorCode,
                    fontSize = 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryTextColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = mutedTextColor,
                    modifier = Modifier.size(13.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Diagnostics & Troubleshooting Card (Solid Pure Monochromatic Structure)
            val cardShape = AzokleRadius.lg
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(cardBg)
                    .border(1.dp, borderCol, cardShape)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(elevatedBg)
                            .border(0.5.dp, borderCol, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = primaryIconColor,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "DIAGNOSTICS & NEXT STEPS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = primaryTextColor,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider(
                    color = borderSubtle,
                    thickness = 0.5.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                error.troubleshootSteps.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${index + 1}.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = primaryTextColor,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = step,
                            fontSize = 12.5.sp,
                            color = secondaryTextColor,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 7. Action Buttons (Try Again & Return to Search)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary Action Button (Try Again)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(AzokleRadius.md)
                        .background(btnPrimaryBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = btnPrimaryText.copy(alpha = 0.2f)),
                            onClick = onRetry
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Try again"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = btnPrimaryText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Try Again",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = btnPrimaryText
                        )
                    }
                }

                // Secondary Action Button (Return to Search)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(AzokleRadius.md)
                        .background(btnSecondaryBg)
                        .border(1.dp, btnSecondaryBorder, AzokleRadius.md)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = btnSecondaryText.copy(alpha = 0.1f)),
                            onClick = onHome
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Return to search"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = btnSecondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Return to Search",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = btnSecondaryText
                        )
                    }
                }

                // Optional SSL Advanced Bypass Button
                if (error.canBypass && onBypass != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AzokleRadius.sm)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = mutedTextColor.copy(alpha = 0.15f)),
                                onClick = onBypass
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Advanced: Proceed to $host (Unsafe)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = mutedTextColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}


