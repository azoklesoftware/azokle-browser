package com.azokle.search.common.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.R

/**
 * Clean, premium, and minimalist notification pill.
 * Floats gracefully below the Omnibar with smooth spring physics
 * and crisp typography & vector iconography.
 */
@Composable
fun AzokleToastHost(
    modifier: Modifier = Modifier,
    extraTopOffset: Dp = 0.dp
) {
    val toastState by ToastManager.currentToast.collectAsState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Standard Omnibar is 58dp tall. We add 8dp margin so the notification sits cleanly below the Omnibar.
    val topPadding = statusBarTop + 66.dp + extraTopOffset

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topPadding, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = toastState != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(150)
            ) + fadeOut(animationSpec = tween(150))
        ) {
            toastState?.let { toast ->
                val accentColor = when (toast.type) {
                    ToastType.SUCCESS -> Color(0xFF10B981) // Emerald
                    ToastType.ERROR -> Color(0xFFEF4444)   // Red
                    ToastType.DOWNLOAD -> Color(0xFF38BDF8)// Sky Blue
                    ToastType.TOR -> Color(0xFFC084FC)     // Tor Purple
                    ToastType.FIRE -> Color(0xFFFF7A00)    // Orange
                    ToastType.COPY -> Color(0xFF818CF8)    // Indigo
                    ToastType.INFO -> Color(0xFF38BDF8)    // Light Blue
                }

                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            ambientColor = Color.Black.copy(alpha = 0.35f),
                            spotColor = Color.Black.copy(alpha = 0.25f)
                        )
                        .clip(CircleShape)
                        .background(Color(0xFF18181C).copy(alpha = 0.95f))
                        .border(
                            width = 0.8.dp,
                            color = Color.White.copy(alpha = 0.14f),
                            shape = CircleShape
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .widthIn(min = 100.dp, max = 340.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (toast.type == ToastType.TOR) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_tor),
                                contentDescription = "Tor",
                                modifier = Modifier.size(15.dp)
                            )
                        } else {
                            val iconVector = when (toast.type) {
                                ToastType.SUCCESS -> Icons.Default.CheckCircle
                                ToastType.ERROR -> Icons.Default.ErrorOutline
                                ToastType.DOWNLOAD -> Icons.Default.Download
                                ToastType.FIRE -> Icons.Default.LocalFireDepartment
                                ToastType.COPY -> Icons.Default.ContentCopy
                                ToastType.INFO -> Icons.Default.Info
                                ToastType.TOR -> null
                            }
                            if (iconVector != null) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(7.dp))

                        Text(
                            text = toast.message,
                            color = Color(0xFFF4F4F5),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
