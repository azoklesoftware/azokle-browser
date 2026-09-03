package com.azokle.search.ui.components

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
import com.azokle.search.data.SEARCH_ENGINE_PROVIDERS
import com.azokle.search.data.SearchEngineConfig
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth

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
        containerColor = colors.background,
        contentColor = colors.textPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.border)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .adaptiveContentWidth(560.dp)
                .padding(horizontal = 18.dp)
                .padding(top = 2.dp, bottom = 32.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header Bar (Clean, Calm, Minimal)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.semantics { heading() }) {
                    Text(
                        text = stringResource(R.string.choose_search_engine),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.search_engine_subtitle),
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                }

                // 48dp Close Target
                Box(
                    modifier = Modifier
                        .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceElevated)
                            .border(1.dp, colors.border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Section Label
            Text(
                text = "DEFAULT SEARCH PROVIDER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = colors.textMuted,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 6.dp)
            )

            // 2. Search Engine Grouped Card
            val shape = AzokleRadius.lg
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(colors.cardBg)
                    .border(1.dp, colors.border, shape)
            ) {
                SEARCH_ENGINE_PROVIDERS.forEachIndexed { index, engine ->
                    val isSelected = selectedEngineId.equals(engine.id, ignoreCase = true)

                    SearchEngineGroupedRow(
                        engine = engine,
                        isSelected = isSelected,
                        colors = colors,
                        onClick = {
                            onSelectEngine(engine.id)
                            Toast.makeText(context, "${engine.name} set as default", Toast.LENGTH_SHORT).show()
                            onDismiss()
                            if (!com.azokle.search.util.DefaultBrowserManager.isDefaultBrowser(context)) {
                                com.azokle.search.util.DefaultBrowserManager.launchSetDefaultBrowser(context)
                            }
                        }
                    )

                    if (index < SEARCH_ENGINE_PROVIDERS.size - 1) {
                        HorizontalDivider(
                            color = colors.border.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(start = 54.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Minimal Footer Notice
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Zero Profiling • Queries routed directly without intermediary tracking",
                    fontSize = 11.sp,
                    color = colors.textMuted,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun SearchEngineGroupedRow(
    engine: SearchEngineConfig,
    isSelected: Boolean,
    colors: com.azokle.search.ui.theme.AzokleThemeColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
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
            // Engine Icon Badge
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated)
                    .border(0.8.dp, if (isSelected) colors.statusAccent.copy(alpha = 0.5f) else colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (engine.iconRes != 0) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(engine.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = engine.name.take(1).uppercase(),
                        fontSize = 13.sp,
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
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = colors.textPrimary
                    )
                    if (engine.isDefault) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.surfaceElevated)
                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                text = "DEFAULT",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textMuted
                            )
                        }
                    }
                }

                Text(
                    text = "${engine.domain} • ${engine.subtitle}",
                    fontSize = 11.5.sp,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = colors.textPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
