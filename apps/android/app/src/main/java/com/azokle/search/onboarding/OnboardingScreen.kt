package com.azokle.search.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.R
import com.azokle.search.common.theme.AudiowideFontFamily
import com.azokle.search.common.theme.AzokleBreakpoints
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.WindowHeightSize
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.common.theme.rememberWindowSize
import com.azokle.search.common.util.DefaultBrowserManager
import com.azokle.search.common.util.SEARCH_ENGINE_PROVIDERS
import com.azokle.search.common.util.SearchEngineConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class OnboardingStepData(
    val stepNumber: String,
    val tag: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isBrandLogo: Boolean = false,
    val isSearchEnginePicker: Boolean = false,
    val isFeatureHighlights: Boolean = false
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    selectedSearchEngineId: String = "azokle",
    onSelectSearchEngine: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colors = AzokleTheme.colors
    var activeSearchEngine by remember { mutableStateOf(selectedSearchEngineId) }

    val defaultBrowserLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        onFinish()
    }
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    var hasAutoShownDefaultPrompt by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 3 && !hasAutoShownDefaultPrompt) {
            hasAutoShownDefaultPrompt = true
            if (!DefaultBrowserManager.isDefaultBrowser(context)) {
                delay(350)
                DefaultBrowserManager.launchSetDefaultBrowser(
                    context = context,
                    launcher = defaultBrowserLauncher
                )
            }
        }
    }

    val steps = listOf(
        OnboardingStepData(
            stepNumber = "01",
            tag = "SOVEREIGN BROWSING",
            title = "Browse Without Compromise",
            description = "Experience lightning-fast browsing with uncompromising privacy, zero telemetry profiling, and leakproof Tor circuit isolation.",
            icon = Icons.Default.Security,
            isBrandLogo = true,
            isSearchEnginePicker = false
        ),
        OnboardingStepData(
            stepNumber = "02",
            tag = "SEARCH PROVIDER",
            title = "Choose Search Engine",
            description = "Select your default search provider. All search queries are dispatched directly without query profiling or tracking.",
            icon = Icons.Default.Search,
            isBrandLogo = false,
            isSearchEnginePicker = true
        ),
        OnboardingStepData(
            stepNumber = "03",
            tag = "QUANTUM SHIELD",
            title = "Total Tracker & Ad Defense",
            description = "Invasive advertisements, cross-site analytics scripts, and cryptominers are neutralized at the engine layer for maximal speed.",
            icon = Icons.Default.Shield,
            isBrandLogo = false,
            isSearchEnginePicker = false,
            isFeatureHighlights = true
        ),
        OnboardingStepData(
            stepNumber = "04",
            tag = "GET STARTED",
            title = "Make Azokle Your Daily Driver",
            description = "Set Azokle as your default Android browser for seamless privacy on every opened link, or jump straight in.",
            icon = Icons.Default.Lock,
            isBrandLogo = false,
            isSearchEnginePicker = false
        )
    )

    val windowSize = rememberWindowSize()
    val isCompactHeight = windowSize.height == WindowHeightSize.COMPACT
    val medallionSize = if (isCompactHeight) 64.dp else 92.dp
    val medallionLogoSize = if (isCompactHeight) 30.sp else 44.sp
    val medallionIconSize = if (isCompactHeight) 28.dp else 38.dp
    val titleFontSize = if (isCompactHeight) 19.sp else 23.sp
    val spacerHeight = if (isCompactHeight) 8.dp else 14.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        // Ambient Radial Vignette Aura
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.statusAccent.copy(alpha = if (colors.isDark) 0.12f else 0.05f),
                            Color.Transparent
                        ),
                        center = Offset.Unspecified,
                        radius = 950f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(AzokleBreakpoints.MaxContentWidth)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = if (isCompactHeight) 6.dp else 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Brand Header Bar + Skip Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "azokle",
                        color = colors.textPrimary,
                        fontFamily = AudiowideFontFamily,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(colors.statusAccent)
                    )
                }

                // 44dp Accessible Skip Action
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 54.dp, minHeight = 44.dp)
                        .clip(AzokleRadius.full)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.1f)),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onFinish()
                            }
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Skip onboarding"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(AzokleRadius.full)
                            .background(colors.surfaceElevated)
                            .border(0.8.dp, colors.borderSubtle, AzokleRadius.full)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.skip),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            // 2. Main Onboarding Pager Carousel
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val step = steps[page]

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Elevated Glassmorphic Medallion
                    Box(
                        modifier = Modifier
                            .size(medallionSize)
                            .shadow(
                                elevation = 16.dp,
                                shape = CircleShape,
                                spotColor = colors.statusAccent.copy(alpha = 0.22f)
                            )
                            .clip(CircleShape)
                            .background(colors.cardBg)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        colors.statusAccent.copy(alpha = 0.45f),
                                        colors.borderSubtle,
                                        colors.surfaceElevated
                                    )
                                ),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (step.isBrandLogo) {
                            Text(
                                text = "a",
                                color = colors.textPrimary,
                                fontFamily = AudiowideFontFamily,
                                fontSize = medallionLogoSize,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = colors.statusAccent,
                                modifier = Modifier.size(medallionIconSize)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacerHeight))

                    // Minimal Capsule Step Tag
                    Box(
                        modifier = Modifier
                            .clip(AzokleRadius.full)
                            .background(colors.surfaceElevated)
                            .border(0.8.dp, colors.borderSubtle, AzokleRadius.full)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(colors.statusAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${step.stepNumber} • ${step.tag}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacerHeight))

                    // Title
                    Text(
                        text = step.title,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = (titleFontSize.value + 5).sp,
                        letterSpacing = (-0.3).sp
                    )

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 4.dp else 6.dp))

                    // Description
                    Text(
                        text = step.description,
                        fontSize = if (isCompactHeight) 12.sp else 13.5.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = if (isCompactHeight) 17.sp else 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // Step 2: Interactive Choose Search Engine Component
                    if (step.isSearchEnginePicker) {
                        Spacer(modifier = Modifier.height(14.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SEARCH_ENGINE_PROVIDERS.forEach { engine ->
                                val isSelected = activeSearchEngine.equals(engine.id, ignoreCase = true)

                                SearchEnginePickerCard(
                                    engine = engine,
                                    isSelected = isSelected,
                                    colors = colors,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        activeSearchEngine = engine.id
                                        onSelectSearchEngine(engine.id)
                                    }
                                )
                            }
                        }
                    }

                    // Step 3: Privacy Feature Badges
                    if (step.isFeatureHighlights) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FeaturePill(
                                icon = Icons.Outlined.Shield,
                                title = "Zero Telemetry",
                                modifier = Modifier.weight(1f)
                            )
                            FeaturePill(
                                icon = Icons.Outlined.VpnKey,
                                title = "Tor Circuit",
                                modifier = Modifier.weight(1f)
                            )
                            FeaturePill(
                                icon = Icons.Outlined.Fingerprint,
                                title = "Anti-Fingerprint",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 3. Bottom Controls & Action Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pagination Spring Indicators (4 steps)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 6.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "stepIndicatorWidth"
                        )

                        Box(
                            modifier = Modifier
                                .height(5.dp)
                                .width(width)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isSelected) colors.statusAccent else colors.borderSubtle
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                val isLastPage = pagerState.currentPage == 3
                val ctaShape = AzokleRadius.full

                if (isLastPage) {
                    val isDefault = DefaultBrowserManager.isDefaultBrowser(context)
                    if (!isDefault) {
                        // 1. Primary: Set as Default Browser
                        ElevatedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                DefaultBrowserManager.launchSetDefaultBrowser(
                                    context = context,
                                    launcher = defaultBrowserLauncher
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(
                                    elevation = 6.dp,
                                    shape = ctaShape,
                                    spotColor = colors.statusAccent.copy(alpha = 0.35f)
                                ),
                            shape = ctaShape,
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = colors.statusAccent,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Set as Default Browser",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. Secondary Ghost Button: Start Browsing
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ctaShape)
                                .background(colors.surfaceElevated)
                                .border(0.8.dp, colors.borderSubtle, ctaShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = colors.textPrimary.copy(alpha = 0.1f)),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onFinish()
                                    }
                                )
                                .padding(vertical = 13.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Start browsing without setting default"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Start Browsing",
                                color = colors.textSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        // Already default: Get Started
                        ElevatedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onFinish()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(
                                    elevation = 6.dp,
                                    shape = ctaShape,
                                    spotColor = colors.statusAccent.copy(alpha = 0.35f)
                                ),
                            shape = ctaShape,
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = colors.statusAccent,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.get_started),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    // Steps 0, 1, 2: Continue Button
                    ElevatedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = ctaShape,
                                spotColor = colors.statusAccent.copy(alpha = 0.35f)
                            ),
                        shape = ctaShape,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = colors.statusAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.btn_continue),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual modern selectable Search Engine card.
 */
@Composable
private fun SearchEnginePickerCard(
    engine: SearchEngineConfig,
    isSelected: Boolean,
    colors: com.azokle.search.common.theme.AzokleThemeColors,
    onClick: () -> Unit
) {
    val cardShape = AzokleRadius.md

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(if (isSelected) colors.statusAccent.copy(alpha = 0.08f) else colors.cardBg)
            .border(
                width = if (isSelected) 1.2.dp else 0.8.dp,
                color = if (isSelected) colors.statusAccent else colors.borderSubtle,
                shape = cardShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.statusAccent.copy(alpha = 0.15f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 11.dp)
            .semantics {
                role = Role.RadioButton
                contentDescription = "${engine.name}, ${engine.subtitle}, ${if (isSelected) "selected" else "not selected"}"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Brand Icon / Monogram Badge
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) colors.statusAccent.copy(alpha = 0.15f)
                        else colors.surfaceElevated
                    )
                    .border(
                        width = 0.8.dp,
                        color = if (isSelected) colors.statusAccent.copy(alpha = 0.5f) else colors.borderSubtle,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (engine.iconRes != 0) {
                    Image(
                        painter = painterResource(engine.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = engine.name.first().uppercase(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) colors.statusAccent else colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = engine.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )

                    if (engine.isDefault) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(AzokleRadius.full)
                                .background(colors.statusAccent.copy(alpha = 0.12f))
                                .border(0.8.dp, colors.statusAccent.copy(alpha = 0.3f), AzokleRadius.full)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "RECOMMENDED",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = colors.statusAccent,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                Text(
                    text = engine.domain,
                    fontSize = 11.5.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Selection Radio Ring
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) colors.statusAccent else Color.Transparent
                )
                .border(
                    width = if (isSelected) 0.dp else 1.5.dp,
                    color = if (isSelected) Color.Transparent else colors.borderSubtle,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Minimalist Feature Highlight Pill.
 */
@Composable
private fun FeaturePill(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    val colors = AzokleTheme.colors

    Column(
        modifier = modifier
            .clip(AzokleRadius.md)
            .background(colors.cardBg)
            .border(0.8.dp, colors.borderSubtle, AzokleRadius.md)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(colors.statusAccent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.statusAccent,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
