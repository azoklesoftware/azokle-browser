package com.azokle.search.engine.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.common.theme.AzokleThemeColors

/**
 * An opaque, theme-reactive surface rendered on top of GeckoView during page transitions.
 * Completely eliminates any visual flashes of previously cached framebuffers while the
 * new URL's DOM is resolving and rendering its first frame.
 */
@Composable
fun PageLoadingTransitionSurface(
    targetUrl: String,
    colors: AzokleThemeColors,
    modifier: Modifier = Modifier
) {
    val displayHost = remember(targetUrl) {
        try {
            val uri = android.net.Uri.parse(targetUrl)
            val host = uri.host
            if (!host.isNullOrBlank()) {
                host.removePrefix("www.")
            } else if (targetUrl.startsWith("about:", ignoreCase = true)) {
                targetUrl
            } else {
                targetUrl.take(32)
            }
        } catch (e: Exception) {
            targetUrl.take(24)
        }
    }

    val isSecure = remember(targetUrl) {
        targetUrl.startsWith("https://", ignoreCase = true)
    }

    // Shimmer wave transition across skeleton elements
    val transition = rememberInfiniteTransition(label = "PageLoadingShimmer")
    val shimmerTranslate by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslation"
    )

    val shimmerBaseColor = if (colors.isDark) Color(0xFF1C1C1E) else Color(0xFFE5E7EB)
    val shimmerHighlightColor = if (colors.isDark) Color(0xFF2C2C2E) else Color(0xFFF3F4F6)

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            shimmerBaseColor,
            shimmerHighlightColor,
            shimmerBaseColor
        ),
        start = Offset(shimmerTranslate, shimmerTranslate),
        end = Offset(shimmerTranslate + 400f, shimmerTranslate + 400f)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Elegant loading indicator pill showing destination host
            if (displayHost.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(colors.surfaceElevated.copy(alpha = 0.92f))
                        .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            strokeWidth = 1.8.dp,
                            color = colors.actionPrimaryBg,
                            trackColor = colors.border
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Icon(
                            imageVector = if (isSecure) Icons.Rounded.Lock else Icons.Rounded.Language,
                            contentDescription = null,
                            tint = if (isSecure) colors.statusSuccess else colors.textMuted,
                            modifier = Modifier.size(13.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = displayHost,
                            color = colors.textPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Realistic web skeleton placeholders (banner + headline + paragraph blocks)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(shimmerBrush)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Headline skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .align(Alignment.Start)
                    .height(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Paragraph skeleton lines with natural staggered widths
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .align(Alignment.Start)
                    .height(13.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .align(Alignment.Start)
                    .height(13.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.74f)
                    .align(Alignment.Start)
                    .height(13.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Secondary content block skeleton
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(shimmerBrush)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}
