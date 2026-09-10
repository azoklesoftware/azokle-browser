package com.azokle.search.common.tour

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
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

    fun start(fromIndex: Int = 0) {
        if (steps.isNotEmpty()) {
            currentStepIndex = fromIndex.coerceIn(0, steps.lastIndex)
            isVisible = true
        }
    }

    fun next() {
        if (currentStepIndex < steps.lastIndex) {
            currentStepIndex++
        } else {
            finish()
        }
    }

    fun previous() {
        if (currentStepIndex > 0) {
            currentStepIndex--
        }
    }

    fun skip() {
        finish()
    }

    fun finish() {
        isVisible = false
        onTourFinished()
    }

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

@Composable
fun rememberTourState(
    steps: List<TourStep>,
    onTourFinished: () -> Unit = {}
): TourState {
    return remember(steps) {
        TourState(steps = steps, onTourFinished = onTourFinished)
    }
}

fun Modifier.tourTarget(
    step: TourStep,
    tourState: TourState
): Modifier = this.onGloballyPositioned { coordinates ->
    tourState.registerTarget(step.id, coordinates)
}

/**
 * Driver.js-grade interactive Spotlight Tour Overlay in Jetpack Compose.
 * Renders an animated cutout spotlight over targeted UI components with
 * an anchored popover card guiding the user through the app on first install.
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
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tourSpotlightLeft"
    )
    val animTop by animateFloatAsState(
        targetValue = targetRect.top,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tourSpotlightTop"
    )
    val animRight by animateFloatAsState(
        targetValue = targetRect.right,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tourSpotlightRight"
    )
    val animBottom by animateFloatAsState(
        targetValue = targetRect.bottom,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tourSpotlightBottom"
    )

    val animatedSpotlightRect = Rect(animLeft, animTop, animRight, animBottom)

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
            drawPath(path = screenPath, color = Color.Black.copy(alpha = 0.80f))

            // Glowing accent stroke around spotlight
            if (animatedSpotlightRect.width > 0 && animatedSpotlightRect.height > 0) {
                if (currentStep.shape == TourShape.CIRCLE) {
                    val radius = (maxOf(animatedSpotlightRect.width, animatedSpotlightRect.height) / 2f)
                    val center = animatedSpotlightRect.center
                    drawCircle(
                        color = Color(0xFF38BDF8),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                } else {
                    drawRoundRect(
                        color = Color(0xFF38BDF8),
                        topLeft = animatedSpotlightRect.topLeft,
                        size = animatedSpotlightRect.size,
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }
        }

        // 2. Interactive Step Popover Card
        val screenHeight = with(density) { androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx() }
        val placeAbove = animatedSpotlightRect.bottom > (screenHeight * 0.55f)

        val popoverYOffset = if (placeAbove) {
            (animatedSpotlightRect.top - with(density) { 200.dp.toPx() }).coerceAtLeast(with(density) { 50.dp.toPx() })
        } else {
            (animatedSpotlightRect.bottom + with(density) { 14.dp.toPx() }).coerceAtMost(screenHeight - with(density) { 240.dp.toPx() })
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = 0, y = popoverYOffset.roundToInt()) }
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, AzokleRadius.xl, ambientColor = Color.Black.copy(alpha = 0.5f), spotColor = Color(0xFF38BDF8).copy(alpha = 0.35f))
                    .clip(AzokleRadius.xl)
                    .background(colors.modalBg)
                    .border(1.2.dp, colors.border, AzokleRadius.xl)
                    .padding(20.dp)
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
                            .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                            .border(0.8.dp, Color(0xFF38BDF8).copy(alpha = 0.35f), AzokleRadius.full)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Step ${tourState.currentStepIndex + 1} of ${tourState.totalSteps}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.textPrimary),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    tourState.skip()
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Tour",
                            tint = colors.textMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Step Title
                Text(
                    text = currentStep.title,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Step Description
                Text(
                    text = currentStep.description,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Popover Actions: Skip / Previous / Next
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
                            }
                        ) {
                            Text(
                                text = "Previous",
                                color = colors.textMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        TextButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                tourState.skip()
                            }
                        ) {
                            Text(
                                text = "Skip Tour",
                                color = colors.textMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Primary Action: Next / Done
                    Box(
                        modifier = Modifier
                            .clip(AzokleRadius.full)
                            .background(colors.actionPrimaryBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = true, color = colors.actionPrimaryText.copy(alpha = 0.25f)),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    tourState.next()
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = if (tourState.isLastStep) "Finish Tour" else "Next Step"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (tourState.isLastStep) "Get Started" else "Next",
                                color = colors.actionPrimaryText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (tourState.isLastStep) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = colors.actionPrimaryText,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
