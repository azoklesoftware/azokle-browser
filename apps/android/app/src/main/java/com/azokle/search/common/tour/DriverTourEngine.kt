/*
 * Copyright (c) 2026 The Azokle Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.azokle.search.common.tour

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.common.theme.AzokleBreakpoints
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import kotlin.math.roundToInt

enum class TourShape {
    ROUNDED_RECT,
    CIRCLE
}

data class TourStep(
    val id: String,
    val title: String,
    val description: String,
    val shape: TourShape = TourShape.ROUNDED_RECT,
    val paddingDp: Float = 6f,
    val cornerRadiusDp: Float = 14f
)

@Stable
class TourState(
    val steps: List<TourStep>,
    private val onTourFinished: () -> Unit = {}
) {
    var isVisible by mutableStateOf(false)
        private set

    var currentStepIndex by mutableStateOf(0)
        private set

    val targetBounds = mutableStateMapOf<String, Rect>()

    val currentStep: TourStep?
        get() = if (isVisible && currentStepIndex in steps.indices) steps[currentStepIndex] else null

    val totalSteps: Int
        get() = steps.size

    val isLastStep: Boolean
        get() = currentStepIndex == steps.lastIndex

    /**
     * Executes the start operation.
     */
    fun start(fromIndex: Int = 0) {
        if (steps.isNotEmpty()) {
            currentStepIndex = fromIndex.coerceIn(0, steps.lastIndex)
            isVisible = true
        }
    }

    /**
     * Executes the next operation.
     */
    fun next() {
        if (currentStepIndex < steps.lastIndex) {
            currentStepIndex++
        } else {
            finish()
        }
    }

    /**
     * Executes the previous operation.
     */
    fun previous() {
        if (currentStepIndex > 0) {
            currentStepIndex--
        }
    }

    /**
     * Executes the skip operation.
     */
    fun skip() {
        finish()
    }

    /**
     * Executes the finish operation.
     */
    fun finish() {
        isVisible = false
        onTourFinished()
    }

    /**
     * Executes the register target operation.
     */
    fun registerTarget(stepId: String, coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            val pos = coordinates.positionInRoot()
            val size = coordinates.size
            targetBounds[stepId] = Rect(
                left = pos.x,
                top = pos.y,
                right = pos.x + size.width,
                bottom = pos.y + size.height
            )
        }
    }
}

/**
 * Composable that renders the remember tour state UI component.
 */
@Composable
fun rememberTourState(
    steps: List<TourStep>,
    onTourFinished: () -> Unit = {}
): TourState {
    return remember(steps) {
        TourState(steps = steps, onTourFinished = onTourFinished)
    }
}

/**
 * Executes the tour target operation.
 */
fun Modifier.tourTarget(
    step: TourStep,
    tourState: TourState
): Modifier = this.onGloballyPositioned { coordinates ->
    tourState.registerTarget(step.id, coordinates)
}

/**
 * Driver.js-grade interactive Spotlight Tour Overlay in Jetpack Compose.
 * Renders an animated cutout spotlight over targeted UI components with
 * an anchored popover card guiding the user through the app.
 */
@Composable
fun DriverTourOverlay(
    tourState: TourState,
    modifier: Modifier = Modifier
) {
    if (!tourState.isVisible) return

    val currentStep = tourState.currentStep ?: return
    val density = LocalDensity.current
    val colors = AzokleTheme.colors
    val haptic = LocalHapticFeedback.current

    // Look up target bounds or default to screen center
    val rawBounds = tourState.targetBounds[currentStep.id]
    val paddingPx = with(density) { currentStep.paddingDp.dp.toPx() }
    val cornerRadiusPx = with(density) { currentStep.cornerRadiusDp.dp.toPx() }

    val targetRect = remember(rawBounds, paddingPx) {
        if (rawBounds != null) {
            Rect(
                left = rawBounds.left - paddingPx,
                top = rawBounds.top - paddingPx,
                right = rawBounds.right + paddingPx,
                bottom = rawBounds.bottom + paddingPx
            )
        } else {
            Rect(0f, 0f, 0f, 0f)
        }
    }

    // Smooth physics-based animation between target element positions
    val animLeft by animateFloatAsState(
        targetValue = targetRect.left,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "tourSpotlightLeft"
    )
    val animTop by animateFloatAsState(
        targetValue = targetRect.top,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "tourSpotlightTop"
    )
    val animRight by animateFloatAsState(
        targetValue = targetRect.right,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "tourSpotlightRight"
    )
    val animBottom by animateFloatAsState(
        targetValue = targetRect.bottom,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "tourSpotlightBottom"
    )

    val animatedSpotlightRect = Rect(animLeft, animTop, animRight, animBottom)

    // Pulse Halo animation around spotlight
    val infiniteTransition = rememberInfiniteTransition(label = "spotlight_halo")
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    val accentColor = colors.statusAccent

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* Intercept touch outside popover */ }
            )
    ) {
        // 1. Dark Backdrop with Cutout Spotlight Path (EvenOdd fill)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val screenPath = Path().apply {
                fillType = PathFillType.EvenOdd
                // Outer entire canvas
                addRect(Rect(0f, 0f, size.width, size.height))

                // Inner cutout spotlight
                if (animatedSpotlightRect.width > 0 && animatedSpotlightRect.height > 0) {
                    if (currentStep.shape == TourShape.CIRCLE) {
                        val radius = (maxOf(animatedSpotlightRect.width, animatedSpotlightRect.height) / 2f)
                        val center = animatedSpotlightRect.center
                        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
                    } else {
                        addRoundRect(
                            RoundRect(
                                rect = animatedSpotlightRect,
                                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
                            )
                        )
                    }
                }
            }

            // Dark semi-transparent scrim
            drawPath(path = screenPath, color = Color.Black.copy(alpha = 0.76f))

            // Multi-Layered Glowing Accent Rings around spotlight
            if (animatedSpotlightRect.width > 0 && animatedSpotlightRect.height > 0) {
                if (currentStep.shape == TourShape.CIRCLE) {
                    val radius = (maxOf(animatedSpotlightRect.width, animatedSpotlightRect.height) / 2f)
                    val center = animatedSpotlightRect.center

                    // Outer soft halo
                    drawCircle(
                        color = accentColor.copy(alpha = haloAlpha),
                        radius = radius + 3.dp.toPx(),
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Inner crisp accent ring
                    drawCircle(
                        color = accentColor,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                } else {
                    // Outer soft halo
                    drawRoundRect(
                        color = accentColor.copy(alpha = haloAlpha),
                        topLeft = Offset(animatedSpotlightRect.left - 2.dp.toPx(), animatedSpotlightRect.top - 2.dp.toPx()),
                        size = Size(animatedSpotlightRect.width + 4.dp.toPx(), animatedSpotlightRect.height + 4.dp.toPx()),
                        cornerRadius = CornerRadius(cornerRadiusPx + 2.dp.toPx(), cornerRadiusPx + 2.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Inner crisp accent ring
                    drawRoundRect(
                        color = accentColor,
                        topLeft = animatedSpotlightRect.topLeft,
                        size = animatedSpotlightRect.size,
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        // 2. Interactive Step Popover Card
        val configuration = LocalConfiguration.current
        val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }
        val placeAbove = animatedSpotlightRect.bottom > (screenHeight * 0.52f)

        val popoverYOffset = if (placeAbove) {
            (animatedSpotlightRect.top - with(density) { 190.dp.toPx() }).coerceAtLeast(with(density) { 48.dp.toPx() })
        } else {
            (animatedSpotlightRect.bottom + with(density) { 14.dp.toPx() }).coerceAtMost(screenHeight - with(density) { 230.dp.toPx() })
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = 0, y = popoverYOffset.roundToInt()) }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val cardShape = AzokleRadius.xl

            Column(
                modifier = Modifier
                    .adaptiveContentWidth(AzokleBreakpoints.MaxSheetWidth)
                    .shadow(
                        elevation = 20.dp,
                        shape = cardShape,
                        spotColor = accentColor.copy(alpha = 0.28f),
                        ambientColor = Color.Black.copy(alpha = 0.5f)
                    )
                    .clip(cardShape)
                    .background(colors.cardBg)
                    .border(1.dp, accentColor.copy(alpha = 0.2f), cardShape)
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                // Popover Header: Step Counter Pill & Close Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(AzokleRadius.full)
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(0.8.dp, accentColor.copy(alpha = 0.35f), AzokleRadius.full)
                            .padding(horizontal = 9.dp, vertical = 3.5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "TOUR • ${tourState.currentStepIndex + 1} / ${tourState.totalSteps}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.6.sp,
                            color = accentColor
                        )
                    }

                    // Accessible Close Tour Action
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, radius = 16.dp, color = colors.textSecondary),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    tourState.skip()
                                }
                            )
                            .semantics {
                                role = Role.Button
                                contentDescription = "Close Tour"
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
                                tint = colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) togetherWith
                                fadeOut(animationSpec = tween(120, easing = FastOutSlowInEasing))
                    },
                    label = "TourStepContent"
                ) { step ->
                    Column {
                        // Step Title
                        Text(
                            text = step.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp,
                            color = colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(5.dp))

                        // Step Description
                        Text(
                            text = step.description,
                            fontSize = 12.5.sp,
                            color = colors.textSecondary,
                            lineHeight = 17.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Popover Actions: Skip/Prev, Dots Indicator, Next/Finish
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (tourState.currentStepIndex > 0) {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                tourState.previous()
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Previous",
                                color = colors.textMuted,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                tourState.skip()
                            },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Skip Tour",
                                color = colors.textMuted,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Mini Progress Dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(tourState.totalSteps) { idx ->
                            val isSelected = tourState.currentStepIndex == idx
                            val dotWidth by animateDpAsState(
                                targetValue = if (isSelected) 14.dp else 4.dp,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "tourDotWidth"
                            )

                            Box(
                                modifier = Modifier
                                    .height(4.dp)
                                    .width(dotWidth)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (isSelected) accentColor else colors.borderSubtle
                                    )
                            )
                        }
                    }

                    // Primary Action: Next / Done
                    ElevatedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            tourState.next()
                        },
                        shape = AzokleRadius.full,
                        modifier = Modifier
                            .height(38.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = AzokleRadius.full,
                                spotColor = accentColor.copy(alpha = 0.35f)
                            ),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (tourState.isLastStep) "Get Started" else "Next",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                imageVector = if (tourState.isLastStep) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
