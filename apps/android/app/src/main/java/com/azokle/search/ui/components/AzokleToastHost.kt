package com.azokle.search.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.azokle.search.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AzokleToastHost(
    modifier: Modifier = Modifier
) {
    val toastState by ToastManager.currentToast.collectAsState()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = toastState != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            toastState?.let { toast ->
                val (accentColor, iconText) = when (toast.type) {
                    ToastType.SUCCESS -> Pair(Color(0xFF10B981), "✓")
                    ToastType.ERROR -> Pair(Color(0xFFEF4444), "✕")
                    ToastType.DOWNLOAD -> Pair(Color(0xFF38BDF8), "📥")
                    ToastType.TOR -> Pair(Color(0xFFC084FC), "")
                    ToastType.FIRE -> Pair(Color(0xFFFF7A00), "🔥")
                    ToastType.COPY -> Pair(Color(0xFF818CF8), "📋")
                    ToastType.INFO -> Pair(Color(0xFF38BDF8), "ℹ")
                }

                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = accentColor.copy(alpha = 0.35f),
                            ambientColor = Color.Black.copy(alpha = 0.5f)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1C1C22).copy(alpha = 0.96f),
                                    Color(0xFF141418).copy(alpha = 0.98f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.6f),
                                    Color.White.copy(alpha = 0.12f),
                                    accentColor.copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .widthIn(min = 120.dp, max = 340.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.18f))
                                .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (toast.type == ToastType.TOR) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_tor),
                                    contentDescription = "Tor",
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = iconText,
                                    fontSize = if (iconText.length > 1) 12.sp else 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = toast.message,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
