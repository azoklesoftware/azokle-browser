package com.azokle.search.ui.components

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/**
 * Minimal, crisp ember spark particle (solid color, zero gradient).
 */
private class PureEmber(
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
 * Premium, Clean, Simple & Solid Fire Incineration Overlay.
 *
 * Designed with pure solid colors (zero gradients):
 * - Immaculate deep pitch obsidian background
 * - Minimalist solid glass disc with crisp amber hairline border
 * - Concentric expanding solid shockwave rings
 * - Pure rising ember sparks with clean alpha falloff
 * - Refined solid status card with crisp typography
 */
@Composable
fun FireIncinerationOverlay(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val smoothDecel = remember { CubicBezierEasing(0.16f, 1f, 0.3f, 1f) }

    // Primary State Drivers
    val veilAlpha = remember { Animatable(0f) }
    val contentScale = remember { Animatable(0.92f) }
    val ring1Scale = remember { Animatable(0.8f) }
    val ring1Alpha = remember { Animatable(0f) }
    val ring2Scale = remember { Animatable(0.6f) }
    val ring2Alpha = remember { Animatable(0f) }

    // Curated solid ember particles (pure colors: Amber, Golden Yellow, Warm Orange)
    val embers = remember {
        val solidColors = listOf(
            Color(0xFFFF9800), // Amber
            Color(0xFFFFB300), // Golden Amber
            Color(0xFFF97316), // Warm Orange
            Color(0xFFFFA000)  // Gold
        )
        List(18) { index ->
            PureEmber(
                xPercent = 0.12f + Random.nextFloat() * 0.76f,
                yPercent = 0.55f + Random.nextFloat() * 0.45f,
                radiusDp = Random.nextFloat() * 1.8f + 1.2f,
                ascentSpeed = Random.nextFloat() * 0.16f + 0.12f,
                horizontalWander = Random.nextFloat() * 0.018f + 0.008f,
                swayPhase = Random.nextFloat() * 6.28f,
                solidColor = solidColors[index % solidColors.size],
                baseAlpha = Random.nextFloat() * 0.3f + 0.7f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "solidEmbers")
    val emberSwayPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "emberSwayPhase"
    )

    LaunchedEffect(Unit) {
        // Tactile Haptic Pulse
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 30, 40, 50)
                val amplitudes = intArrayOf(0, 100, 0, 190)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(180)
            }
        } catch (_: Exception) {
            // Graceful fallback
        }

        // Phase 1: Smooth Reveal
        veilAlpha.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
        contentScale.animateTo(1f, tween(340, easing = smoothDecel))

        // Expanding Solid Shockwave Rings
        ring1Alpha.snapTo(0.6f)
        ring1Scale.animateTo(2.0f, tween(550, easing = FastOutSlowInEasing))
        ring1Alpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing))

        ring2Alpha.snapTo(0.35f)
        ring2Scale.animateTo(2.7f, tween(700, easing = FastOutSlowInEasing))
        ring2Alpha.animateTo(0f, tween(350, easing = FastOutSlowInEasing))

        // Phase 2: Steady Hold
        delay(550)

        // Phase 3: Smooth Exit to Clean Slate
        veilAlpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
        delay(30)
        onComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(veilAlpha.value)
            .background(Color(0xFF000000))
    ) {
        // 1. Clean Canvas: Concentric Solid Rings & Crisp Solid Embers
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val globalAlpha = veilAlpha.value
            val centerPoint = Offset(w / 2f, h * 0.44f)

            // 1A. Expanding Solid Ring 1
            if (ring1Alpha.value > 0.01f) {
                val r1 = 42.dp.toPx() * ring1Scale.value
                drawCircle(
                    color = Color(0xFFFF9800).copy(alpha = ring1Alpha.value * 0.6f),
                    radius = r1,
                    center = centerPoint,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // 1B. Expanding Solid Ring 2
            if (ring2Alpha.value > 0.01f) {
                val r2 = 42.dp.toPx() * ring2Scale.value
                drawCircle(
                    color = Color(0xFFF97316).copy(alpha = ring2Alpha.value * 0.4f),
                    radius = r2,
                    center = centerPoint,
                    style = Stroke(width = 1.0.dp.toPx())
                )
            }

            // 1C. Clean Solid Embers
            embers.forEach { ember ->
                ember.yPercent -= (ember.ascentSpeed * 0.012f)
                val sway = sin((ember.yPercent * 8f + ember.swayPhase + emberSwayPhase).toDouble()).toFloat()
                val drawX = (ember.xPercent + sway * ember.horizontalWander) * w
                val drawY = ember.yPercent * h

                if (drawY in -20f..h) {
                    val progress = (1f - (drawY / h)).coerceIn(0f, 1f)
                    val fadeAlpha = (ember.baseAlpha * (1f - (progress * 0.65f)) * globalAlpha).coerceIn(0f, 1f)
                    val rPx = ember.radiusDp.dp.toPx()

                    drawCircle(
                        color = ember.solidColor.copy(alpha = fadeAlpha),
                        radius = rPx,
                        center = Offset(drawX, drawY)
                    )
                }
            }
        }

        // 2. Pure, Minimalist Luxury Centerpiece
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
                // Solid Minimalist Flame Disc
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141416))
                        .border(1.5.dp, Color(0xFFFF9800), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Session Incinerated",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Solid Minimalist Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF141416))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = stringResource(com.azokle.search.R.string.incinerated_title),
                                color = Color(0xFFFFFFFF),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = stringResource(com.azokle.search.R.string.incinerated_sub),
                            color = Color(0xFF9E9EA8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.2.sp
                        )
                    }
                }
            }
        }
    }
}
