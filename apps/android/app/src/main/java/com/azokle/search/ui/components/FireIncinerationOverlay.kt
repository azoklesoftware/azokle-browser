package com.azokle.search.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
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
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

private data class LuxuryEmber(
    var xPercent: Float,
    var yPercent: Float,
    val size: Float,
    val speed: Float,
    val color: Color,
    val wobbleOffset: Float
)

@Composable
fun FireIncinerationOverlay(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val contentAlpha = remember { Animatable(0f) }
    val cardScale = remember { Animatable(0.88f) }
    val flameRise = remember { Animatable(0f) }

    // 80 Dynamic floating fiery embers & sparks
    val embers = remember {
        List(80) {
            LuxuryEmber(
                xPercent = Random.nextFloat(),
                yPercent = 0.5f + Random.nextFloat() * 0.5f,
                size = Random.nextFloat() * 4.5f + 1.5f,
                speed = Random.nextFloat() * 0.38f + 0.22f,
                color = when (Random.nextInt(4)) {
                    0 -> Color(0xFFFF3300) // Neon Crimson
                    1 -> Color(0xFFFF6600) // Deep Orange
                    2 -> Color(0xFFFFB800) // Golden Amber
                    else -> Color(0xFFFFF0D0) // White-Gold Hot
                },
                wobbleOffset = Random.nextFloat() * 6.28f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "flamePulse")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    val flamePath = remember { Path() }
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    // Trigger Haptics and Smooth Sequence
    LaunchedEffect(Unit) {
        // Rhythmic Haptic Pulse
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 30, 40, 50, 40, 80, 50, 100)
                val amplitudes = intArrayOf(0, 100, 0, 160, 0, 220, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(280)
            }
        } catch (e: Exception) {
            // Ignore
        }

        // 1. Smooth Fade in obsidian veil & rise flame crest
        contentAlpha.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
        flameRise.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        cardScale.animateTo(1f, tween(400, easing = FastOutSlowInEasing))

        delay(450)

        // 2. Smooth dissipation to clean slate
        contentAlpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
        delay(60)
        onComplete()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(contentAlpha.value)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF160907).copy(alpha = 0.96f),
                        Color(0xFF09060A).copy(alpha = 0.98f),
                        Color(0xFF030204)
                    ),
                    center = Offset.Unspecified,
                    radius = 1200f
                )
            )
    ) {
        // 1. Procedural Flame Waves & Hot Floating Embers Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rise = flameRise.value

            // 1A. Deep Volcanic Bottom Ambient Gradient (Subtle crimson/orange only at the very bottom)
            val ambientHeight = h * 0.38f * rise
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0x22FF2200),
                        Color(0x66FF5500),
                        Color(0xBBFF7700)
                    ),
                    startY = h - ambientHeight,
                    endY = h
                ),
                size = size
            )

            // 1B. Dynamic Wave Flame Crests (Reusing persistent Path to avoid frame allocations)
            flamePath.reset()
            flamePath.moveTo(0f, h)
            val stepCount = 16
            for (i in 0..stepCount) {
                val x = (i.toFloat() / stepCount) * w
                val sineVal = sin((i * 0.9f + wavePhase).toDouble()).toFloat() * (h * 0.035f * rise)
                val y = (h - (ambientHeight * 0.65f)) + sineVal
                if (i == 0) {
                    flamePath.lineTo(x, y)
                } else {
                    val prevX = ((i - 1).toFloat() / stepCount) * w
                    val prevSine = sin(((i - 1) * 0.9f + wavePhase).toDouble()).toFloat() * (h * 0.035f * rise)
                    val prevY = (h - (ambientHeight * 0.65f)) + prevSine
                    val midX = (prevX + x) / 2f
                    val midY = (prevY + y) / 2f
                    flamePath.quadraticTo(prevX, prevY, midX, midY)
                }
            }
            flamePath.lineTo(w, h)
            flamePath.close()

            drawPath(
                path = flamePath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x88FF2200),
                        Color(0xAAFF5500),
                        Color(0xDDFF8800)
                    ),
                    startY = h - ambientHeight,
                    endY = h
                ),
                style = Fill
            )

            // 1C. 80+ Floating Radiant Embers & Rising Sparks
            embers.forEach { ember ->
                ember.yPercent -= (ember.speed * 0.016f * (rise + 0.4f))
                val wobbleX = sin((ember.yPercent * 14f + ember.wobbleOffset + wavePhase).toDouble()).toFloat() * 0.03f
                val drawX = (ember.xPercent + wobbleX) * w
                val drawY = ember.yPercent * h

                if (drawY in 0f..h) {
                    // Soft radiant aura
                    drawCircle(
                        color = ember.color.copy(alpha = 0.2f * contentAlpha.value),
                        radius = ember.size * 2.5f,
                        center = Offset(drawX, drawY)
                    )
                    // Core hot spark
                    drawCircle(
                        color = ember.color.copy(alpha = 0.95f * contentAlpha.value),
                        radius = ember.size,
                        center = Offset(drawX, drawY)
                    )
                }
            }
        }

        // 2. Luxury Glassmorphic Incineration Status HUD
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
                    .scale(cardScale.value)
                    .padding(horizontal = 24.dp)
            ) {
                // Glowing Flame Icon Orb
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(32.dp, CircleShape, spotColor = Color(0xFFFF5500))
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0xFFFFB800),
                                    Color(0xFFFF4400),
                                    Color(0xFF881100)
                                )
                            )
                        )
                        .border(
                            2.dp,
                            Brush.linearGradient(listOf(Color(0xFFFFDD88), Color(0xFFFF3300))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Fire",
                        tint = Color.White,
                        modifier = Modifier
                            .size(42.dp)
                            .scale(glowPulse)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Luxury Glassmorphic Pill Card
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF131118).copy(alpha = 0.92f))
                        .border(
                            1.dp,
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFFF5500).copy(alpha = 0.6f),
                                    Color(0xFFFFB800).copy(alpha = 0.8f),
                                    Color(0xFFFF3366).copy(alpha = 0.6f)
                                )
                            ),
                            RoundedCornerShape(22.dp)
                        )
                        .padding(horizontal = 22.dp, vertical = 13.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Purged",
                                tint = Color(0xFF22C55E),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.azokle.search.R.string.incinerated_title),
                                color = Color(0xFFFFFAF0),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = androidx.compose.ui.res.stringResource(com.azokle.search.R.string.incinerated_sub),
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
