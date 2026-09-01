package com.azokle.search.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.R
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth
import com.azokle.search.viewmodel.HomeViewModel

@Composable
fun DiscoverScreen(
    viewModel: HomeViewModel,
    onTopicClick: (String) -> Unit = {},
    onNewsClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AzokleTheme.colors
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Minimalist Ambient Radial Glow
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
                        radius = 800f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .adaptiveContentWidth(640.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. Clean Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.cardBg)
                        .border(1.dp, colors.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.semantics { heading() }
                ) {
                    Text(
                        text = "DISCOVER",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textPrimary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(colors.statusAccent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Hero Centerpiece Medallion
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(
                        elevation = if (colors.isDark) 24.dp else 10.dp,
                        shape = CircleShape,
                        spotColor = colors.statusAccent.copy(alpha = if (colors.isDark) 0.25f else 0.15f)
                    )
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                colors.statusAccent.copy(alpha = 0.5f),
                                colors.border,
                                colors.surfaceElevated
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 3. Minimalist "COMING SOON" Status Capsule
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
                        text = stringResource(R.string.discover_pill),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Hero Title & Subtitle
            Text(
                text = stringResource(R.string.discover_coming_soon),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp,
                letterSpacing = (-0.3).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.discover_coming_soon_sub),
                fontSize = 13.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 14.dp)
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 5. Feature Highlight Preview Cards (What to Expect)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DiscoverFeatureCard(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.feature_private_feed),
                    description = stringResource(R.string.feature_private_feed_sub),
                    colors = colors
                )

                DiscoverFeatureCard(
                    icon = Icons.Default.AutoAwesome,
                    title = stringResource(R.string.feature_ai_summaries),
                    description = stringResource(R.string.feature_ai_summaries_sub),
                    colors = colors
                )

                DiscoverFeatureCard(
                    icon = Icons.Default.Lock,
                    title = stringResource(R.string.feature_local_privacy),
                    description = stringResource(R.string.feature_local_privacy_sub),
                    colors = colors
                )
            }

            Spacer(modifier = Modifier.height(96.dp))
            }
        }
    }
}

@Composable
private fun DiscoverFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    colors: com.azokle.search.ui.theme.AzokleThemeColors,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AzokleRadius.lg)
            .background(colors.cardBg)
            .border(1.dp, colors.border, AzokleRadius.lg)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(AzokleRadius.md)
                .background(colors.surface)
                .border(1.dp, colors.border, AzokleRadius.md),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = colors.textSecondary,
                lineHeight = 16.sp
            )
        }
    }
}
