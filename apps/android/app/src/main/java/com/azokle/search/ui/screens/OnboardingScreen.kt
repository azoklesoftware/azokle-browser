package com.azokle.search.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.R
import com.azokle.search.ui.theme.AudiowideFontFamily
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.WindowHeightSize
import com.azokle.search.ui.theme.adaptiveContentWidth
import com.azokle.search.ui.theme.rememberWindowSize
import com.azokle.search.util.DefaultBrowserManager
import kotlinx.coroutines.launch

private data class OnboardingStepData(
    val stepNumber: String,
    val tag: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isBrandLogo: Boolean = false
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = AzokleTheme.colors
    val defaultBrowserLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        onFinish()
    }
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    val steps = listOf(
        OnboardingStepData(
            stepNumber = "01",
            tag = stringResource(R.string.tag_private_search),
            title = stringResource(R.string.onboarding_title_1),
            description = stringResource(R.string.onboarding_sub_1),
            icon = Icons.Default.Security,
            isBrandLogo = true
        ),
        OnboardingStepData(
            stepNumber = "02",
            tag = stringResource(R.string.tag_ad_blocker),
            title = stringResource(R.string.onboarding_title_2),
            description = stringResource(R.string.onboarding_sub_2),
            icon = Icons.Default.Shield,
            isBrandLogo = false
        ),
        OnboardingStepData(
            stepNumber = "03",
            tag = stringResource(R.string.tag_one_tap_cleanup),
            title = stringResource(R.string.onboarding_title_3),
            description = stringResource(R.string.onboarding_sub_3),
            icon = Icons.Default.Lock,
            isBrandLogo = false
        )
    )

    val windowSize = rememberWindowSize()
    val isCompactHeight = windowSize.height == WindowHeightSize.COMPACT
    val medallionSize = if (isCompactHeight) 76.dp else 126.dp
    val medallionLogoSize = if (isCompactHeight) 38.sp else 58.sp
    val medallionIconSize = if (isCompactHeight) 32.dp else 46.dp
    val titleFontSize = if (isCompactHeight) 20.sp else 26.sp
    val spacerHeight = if (isCompactHeight) 8.dp else 16.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        // Ambient Radial Vignette
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            colors.statusAccent.copy(alpha = if (colors.isDark) 0.08f else 0.04f),
                            Color.Transparent
                        ),
                        center = Offset.Unspecified,
                        radius = 900f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveContentWidth(640.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = if (isCompactHeight) 8.dp else 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar (Brand Monogram + Minimal Skip Pill)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "azokle",
                        color = colors.textPrimary,
                        fontFamily = AudiowideFontFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(colors.statusAccent)
                    )
                }

                // 48dp Touch Target Skip Pill Button
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 54.dp, minHeight = 44.dp)
                        .clip(AzokleRadius.full)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.1f)),
                            onClick = onFinish
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
                            .background(colors.cardBg)
                            .border(1.dp, colors.border, AzokleRadius.full)
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

            // 2. Center Content Carousel
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
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Medallion
                    Box(
                        modifier = Modifier
                            .size(medallionSize)
                            .shadow(24.dp, CircleShape, spotColor = colors.statusAccent.copy(alpha = 0.2f))
                            .clip(CircleShape)
                            .background(colors.surface)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        colors.statusAccent.copy(alpha = 0.4f),
                                        colors.border,
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
                                tint = colors.textPrimary,
                                modifier = Modifier.size(medallionIconSize)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacerHeight))

                    // Tag Capsule
                    Box(
                        modifier = Modifier
                            .clip(AzokleRadius.full)
                            .background(colors.surface)
                            .border(1.dp, colors.border, AzokleRadius.full)
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(colors.statusAccent)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "${step.stepNumber} • ${step.tag}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                letterSpacing = 1.sp
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
                        lineHeight = (titleFontSize.value + 6).sp,
                        letterSpacing = (-0.3).sp
                    )

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 4.dp else 10.dp))

                    // Description
                    Text(
                        text = step.description,
                        fontSize = if (isCompactHeight) 12.sp else 13.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = if (isCompactHeight) 17.sp else 20.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // 3. Bottom Controls & CTA Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pagination Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 26.dp else 7.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "indicatorWidth"
                        )

                        Box(
                            modifier = Modifier
                                .height(5.dp)
                                .width(width)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (isSelected) colors.actionPrimaryBg else colors.border
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // CTA Button
                val isLastPage = pagerState.currentPage == 2
                val ctaShape = AzokleRadius.full

                if (isLastPage) {
                    val isDefault = DefaultBrowserManager.isDefaultBrowser(context)
                    if (!isDefault) {
                        // 1. Primary: Set as Default Browser
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, ctaShape)
                                .clip(ctaShape)
                                .background(colors.actionPrimaryBg)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = colors.actionPrimaryText.copy(alpha = 0.2f))
                                ) {
                                    DefaultBrowserManager.launchSetDefaultBrowser(
                                        context = context,
                                        launcher = defaultBrowserLauncher
                                    )
                                }
                                .padding(vertical = 15.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Set as default browser"
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = colors.actionPrimaryText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Set as Default Browser",
                                color = colors.actionPrimaryText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. Secondary Ghost Button: Start Browsing (Not Now)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ctaShape)
                                .background(colors.surface)
                                .border(1.dp, colors.border, ctaShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = colors.textPrimary.copy(alpha = 0.1f)),
                                    onClick = onFinish
                                )
                                .padding(vertical = 13.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Start browsing without setting default"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Start Browsing (Not Now)",
                                color = colors.textSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        // Already default
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, ctaShape)
                                .clip(ctaShape)
                                .background(colors.actionPrimaryBg)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(color = colors.actionPrimaryText.copy(alpha = 0.2f)),
                                    onClick = onFinish
                                )
                                .padding(vertical = 15.dp)
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "Get started"
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.get_started),
                                color = colors.actionPrimaryText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.2.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = colors.actionPrimaryText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    // Page 0 & 1: Continue Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, ctaShape)
                            .clip(ctaShape)
                            .background(colors.actionPrimaryBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = colors.actionPrimaryText.copy(alpha = 0.2f))
                            ) {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                            .padding(vertical = 15.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Continue to next step"
                            },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.btn_continue),
                            color = colors.actionPrimaryText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = colors.actionPrimaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
