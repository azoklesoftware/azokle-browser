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

package com.azokle.search.search

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.R
import com.azokle.search.common.theme.AzokleRadius
import com.azokle.search.common.theme.AzokleTheme
import com.azokle.search.common.theme.adaptiveContentWidth
import com.azokle.search.common.util.DefaultBrowserManager
import com.azokle.search.common.util.SEARCH_ENGINE_PROVIDERS
import com.azokle.search.common.util.SearchEngineConfig

/**
 * Composable that renders the choose search engine sheet UI component.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseSearchEngineSheet(
    selectedEngineId: String,
    onSelectEngine: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = AzokleTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBg,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.borderSubtle)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(560.dp)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.choose_search_engine),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { heading() }
                )

                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 36.dp, minHeight = 36.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false, radius = 18.dp, color = colors.textSecondary),
                            onClick = onDismiss
                        )
                        .semantics {
                            role = Role.Button
                            contentDescription = "Close search engine picker"
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
                            tint = colors.textPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Engines List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AzokleRadius.lg)
                    .background(colors.surfaceElevated)
                    .border(0.8.dp, colors.borderSubtle, AzokleRadius.lg)
            ) {
                SEARCH_ENGINE_PROVIDERS.forEachIndexed { index, engine ->
                    val isSelected = selectedEngineId.equals(engine.id, ignoreCase = true)

                    SearchEngineItemRow(
                        engine = engine,
                        isSelected = isSelected,
                        colors = colors,
                        onClick = {
                            onSelectEngine(engine.id)
                            Toast.makeText(context, "${engine.name} set as default", Toast.LENGTH_SHORT).show()
                            onDismiss()
                            if (!DefaultBrowserManager.isDefaultBrowser(context)) {
                                DefaultBrowserManager.launchSetDefaultBrowser(context)
                            }
                        }
                    )

                    if (index < SEARCH_ENGINE_PROVIDERS.size - 1) {
                        HorizontalDivider(
                            color = colors.borderSubtle,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 52.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composable that renders the search engine item row UI component.
 */
@Composable
private fun SearchEngineItemRow(
    engine: SearchEngineConfig,
    isSelected: Boolean,
    colors: com.azokle.search.common.theme.AzokleThemeColors,
    onClick: () -> Unit
) {
    val rowBackground by animateColorAsState(
        targetValue = if (isSelected) colors.actionPrimaryBg.copy(alpha = 0.06f) else Color.Transparent,
        animationSpec = tween(durationMillis = 180),
        label = "EngineRowBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 52.dp)
            .background(rowBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics {
                role = Role.RadioButton
                contentDescription = "${engine.name}, ${engine.domain}, ${if (isSelected) "selected" else "not selected"}"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(AzokleRadius.sm)
                    .background(colors.cardBg)
                    .border(
                        0.8.dp,
                        if (isSelected) colors.actionPrimaryBg.copy(alpha = 0.35f) else colors.borderSubtle,
                        AzokleRadius.sm
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (engine.iconRes != 0) {
                    Image(
                        painter = painterResource(engine.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = engine.name.take(1).uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f, fill = false)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = engine.name,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = colors.textPrimary
                    )
                    if (engine.isDefault) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.actionPrimaryBg.copy(alpha = 0.12f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "DEFAULT",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.actionPrimaryText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(1.dp))

                Text(
                    text = engine.domain,
                    fontSize = 11.5.sp,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Checkmark indicator
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isSelected) colors.actionPrimaryBg else Color.Transparent)
                .border(
                    width = if (isSelected) 0.dp else 1.2.dp,
                    color = if (isSelected) Color.Transparent else colors.borderSubtle,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = colors.actionPrimaryText,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
