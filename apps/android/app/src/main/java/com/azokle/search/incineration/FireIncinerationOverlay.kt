package com.azokle.search.incineration

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/**
 * Minimalist ember spark particle for clean micro-animations.
 */
private class MinimalEmber(
    var xPercent: Float,
    var yPercent: Float,
    val radiusDp: Float,
    val ascentSpeed: Float,
    val horizontalWander: Float,
    val swayPhase: Float,
    val solidColor: Color,
    val baseAlpha: Float
)

/**
 * Ultra-Minimalist, Clean & Snappy Data Incineration Overlay.
 *
 * Features:
 * - Deep pitch-black background fade
 * - Minimalist glowing flame disc with crisp hairline border
 * - Smooth hairline expanding shockwave ring
 * - Curated subtle rising micro-embers
 * - Clean, minimal typography without visual noise
 * - Responsive tactile haptic feedback
 */
@Composable
fun FireIncinerationOverlay(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val smoothDecel = remember { CubicBezierEasing(0.16f, 1f, 0.3f, 1f) }

    // Primary Animation Drivers
    val veilAlpha = remember { Animatable(0f) }
    val contentScale = remember { Animatable(0.88f) }
    val ringScale = remember { Animatable(0.7f) }
    val ringAlpha = remember { Animatable(0f) }

    // Curated micro-embers (subtle, clean, minimal)
    val embers = remember {
        val colors = listOf(
            Color(0xFFFF9800), // Amber
            Color(0xFFF97316), // Orange
            Color(0xFFFFA000)  // Golden amber
        )
        List(12) { index ->
            MinimalEmber(
                xPercent = 0.2f + Random.nextFloat() * 0.6f,
                yPercent = 0.52f + Random.nextFloat() * 0.4f,
                radiusDp = Random.nextFloat() * 1.4f + 0.8f,
                ascentSpeed = Random.nextFloat() * 0.14f + 0.10f,
                horizontalWander = Random.nextFloat() * 0.015f + 0.006f,
                swayPhase = Random.nextFloat() * 6.28f,
                solidColor = colors[index % colors.size],
                baseAlpha = Random.nextFloat() * 0.3f + 0.65f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "microEmbers")
    val emberSwayPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "emberSwayPhase"
    )

    LaunchedEffect(Unit) {
        // 1. Tactile Haptic Pulse
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 25, 35, 45)
                val amplitudes = intArrayOf(0, 90, 0, 180)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(160)
            }
        } catch (_: Exception) {
            // Graceful fallback
        }

        // 2. Smooth Reveal
        veilAlpha.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
        contentScale.animateTo(1f, tween(280, easing = smoothDecel))

        // Expanding Hairline Shockwave
        ringAlpha.snapTo(0.5f)
        ringScale.animateTo(2.2f, tween(480, easing = FastOutSlowInEasing))
        ringAlpha.animateTo(0f, tween(260, easing = FastOutSlowInEasing))

        // 3. Steady Hold
        delay(480)

        // 4. Smooth Exit
        veilAlpha.animateTo(0f, tween(240, easing = FastOutSlowInEasing))
        delay(20)
        onComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(veilAlpha.value)
            .background(Color(0xFF000000))
    ) {
        // 1. Minimal Canvas: Expanding Hairline Ring & Micro-Embers
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val globalAlpha = veilAlpha.value
            val centerPoint = Offset(w / 2f, h * 0.46f)

            // Expanding Hairline Ring
            if (ringAlpha.value > 0.01f) {
                val r = 36.dp.toPx() * ringScale.value
                drawCircle(
                    color = Color(0xFFFF9800).copy(alpha = ringAlpha.value * 0.5f),
                    radius = r,
                    center = centerPoint,
                    style = Stroke(width = 1.0.dp.toPx())
                )
            }

            // Clean Floating Micro-Embers
            embers.forEach { ember ->
                ember.yPercent -= (ember.ascentSpeed * 0.012f)
                val sway = sin((ember.yPercent * 8f + ember.swayPhase + emberSwayPhase).toDouble()).toFloat()
                val drawX = (ember.xPercent + sway * ember.horizontalWander) * w
                val drawY = ember.yPercent * h

                if (drawY in -20f..h) {
                    val progress = (1f - (drawY / h)).coerceIn(0f, 1f)
                    val fadeAlpha = (ember.baseAlpha * (1f - (progress * 0.6f)) * globalAlpha).coerceIn(0f, 1f)
                    val rPx = ember.radiusDp.dp.toPx()

                    drawCircle(
                        color = ember.solidColor.copy(alpha = fadeAlpha),
                        radius = rPx,
                        center = Offset(drawX, drawY)
                    )
                }
            }
        }

        // 2. Clean, Minimal Centerpiece
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(contentScale.value)
                    .padding(horizontal = 24.dp)
            ) {
                // Minimalist Flame Badge
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141416))
                        .border(1.2.dp, Color(0xFFFF9800), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Data Burned",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Clean Minimalist Title
                Text(
                    text = "Browsing Data Burned",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Clean Minimalist Subtitle
                Text(
                    text = "Tabs, history & session wiped",
                    color = Color(0xFF9E9EA8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
