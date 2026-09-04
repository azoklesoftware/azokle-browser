package com.azokle.search.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azokle.search.R
import com.azokle.search.browser.tabs.TabManager
import com.azokle.search.browser.ui.SiteSettingsSheet
import com.azokle.search.data.PreferencesManager
import com.azokle.search.ui.theme.AzokleRadius
import com.azokle.search.ui.theme.AzokleTheme
import com.azokle.search.ui.theme.adaptiveContentWidth
import com.azokle.search.util.LocaleHelper

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults

data class ThemeOption(
    val id: String,
    @param:androidx.annotation.StringRes val labelRes: Int,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    selectedThemeMode: String = "system",
    onThemeModeChange: (String) -> Unit = {},
    selectedDohProvider: String = "cloudflare",
    onDohProviderChange: (String) -> Unit = {},
    selectedSearchEngine: String = "azokle",
    onOpenSearchEngine: () -> Unit = {},
    selectedLanguageCode: String = "system",
    onOpenLanguage: () -> Unit = {},
    onDismiss: () -> Unit,
    onOpenPro: () -> Unit = {},
    onOpenPrivacyPolicy: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    onReplayTour: (() -> Unit)? = null,
    onReplayHomeTour: (() -> Unit)? = null,
    onReplayTabsTour: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = AzokleTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    var isDohDropdownExpanded by remember { mutableStateOf(false) }
    var showSiteSettingsSheet by remember { mutableStateOf(false) }

    val themeOptions = listOf(
        ThemeOption("system", R.string.theme_system, Icons.Default.PhoneAndroid),
        ThemeOption("dark", R.string.theme_dark, Icons.Default.DarkMode),
        ThemeOption("light", R.string.theme_light, Icons.Default.LightMode)
    )

    val dohProviders = listOf(
        Triple("cloudflare", "Cloudflare 1.1.1.1", "Fastest zero-log Anycast encrypted DNS"),
        Triple("quad9", "Quad9 (9.9.9.9)", "Swiss non-profit threat & malware blocking"),
        Triple("adguard", "AdGuard DNS", "Network-level ad & tracker filter"),
        Triple("system", "System Default DNS", "Standard ISP-provided name servers")
    )

    val activeDoh = dohProviders.find { it.first == selectedDohProvider } ?: dohProviders.first()
    val activeEngine = com.azokle.search.data.SEARCH_ENGINE_PROVIDERS.find { it.id == selectedSearchEngine }
        ?: com.azokle.search.data.SEARCH_ENGINE_PROVIDERS[0]
    val activeLang = com.azokle.search.data.SUPPORTED_LANGUAGES.find { it.code == selectedLanguageCode }
        ?: com.azokle.search.data.SUPPORTED_LANGUAGES.first()

    val arrowRotation by animateFloatAsState(
        targetValue = if (isDohDropdownExpanded) 180f else 0f,
        label = "DohArrowRotation"
    )

    val localizedContext = remember(selectedLanguageCode) {
        LocaleHelper.applyLocale(context, selectedLanguageCode)
    }
    val localizedConfiguration = remember(selectedLanguageCode, localizedContext) {
        localizedContext.resources.configuration
    }
    val layoutDirection = remember(selectedLanguageCode) {
        val loc = LocaleHelper.getLocale(selectedLanguageCode)
        if (LocaleHelper.isRtl(loc)) LayoutDirection.Rtl else LayoutDirection.Ltr
    }

    var isDefaultBrowser by remember {
        mutableStateOf(com.azokle.search.util.DefaultBrowserManager.isDefaultBrowser(context))
    }

    val defaultBrowserLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultBrowser = com.azokle.search.util.DefaultBrowserManager.isDefaultBrowser(context)
    }

    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        isDefaultBrowser = com.azokle.search.util.DefaultBrowserManager.isDefaultBrowser(context)
        onPauseOrDispose {}
    }

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
        CompositionLocalProvider(
            LocalContext provides localizedContext,
            LocalConfiguration provides localizedConfiguration,
            LocalLayoutDirection provides layoutDirection
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
                // Header Bar (Clean, Calm, Minimal)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.semantics { heading() }) {
                        Text(
                            text = stringResource(R.string.settings_title),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = stringResource(R.string.settings_subtitle),
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }

                    // 48dp Close Delegate
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
                                contentDescription = "Close settings"
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

                // ==========================================
                // SECTION 1: GENERAL PREFERENCES (Grouped Card)
                // ==========================================
                SectionHeader(title = "GENERAL")

                SettingsGroupCard(colors = colors) {
                    // 1A. Theme Mode Segmented Picker
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.section_theme),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AzokleRadius.md)
                                .background(colors.surface)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            themeOptions.forEach { option ->
                                val isSelected = selectedThemeMode == option.id
                                val label = stringResource(option.labelRes)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .sizeIn(minHeight = 38.dp)
                                        .clip(AzokleRadius.sm)
                                        .background(if (isSelected) colors.actionPrimaryBg else Color.Transparent)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(bounded = true, color = colors.actionPrimaryText.copy(alpha = 0.2f))
                                        ) { onThemeModeChange(option.id) }
                                        .semantics {
                                            role = Role.Tab
                                            contentDescription = "$label theme"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) colors.actionPrimaryText else colors.textSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) colors.actionPrimaryText else colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    SettingsDivider(colors = colors)

                    // 1B. Default Browser
                    SettingsRowItem(
                        icon = Icons.Default.Public,
                        title = "Default Browser",
                        subtitle = if (isDefaultBrowser) "Azokle is your default browser" else "Open links securely in Azokle",
                        trailingContent = {
                            if (isDefaultBrowser) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = colors.statusSuccess,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(AzokleRadius.sm)
                                        .background(colors.actionPrimaryBg)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "Set",
                                        color = colors.actionPrimaryText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        },
                        colors = colors,
                        onClick = {
                            if (!isDefaultBrowser) {
                                com.azokle.search.util.DefaultBrowserManager.launchSetDefaultBrowser(
                                    context = context,
                                    launcher = defaultBrowserLauncher
                                )
                            } else {
                                try {
                                    val intent = com.azokle.search.util.DefaultBrowserManager.createManageDefaultAppsIntent()
                                    defaultBrowserLauncher.launch(intent)
                                } catch (e: Exception) {}
                            }
                        }
                    )

                    SettingsDivider(colors = colors)

                    // 1C. Search Engine
                    SettingsRowItem(
                        icon = Icons.Default.Search,
                        title = stringResource(R.string.choose_search_engine),
                        subtitle = activeEngine.name,
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeEngine.domain,
                                    fontSize = 12.sp,
                                    color = colors.textMuted
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        colors = colors,
                        onClick = {
                            onDismiss()
                            onOpenSearchEngine()
                        }
                    )

                    SettingsDivider(colors = colors)

                    // 1D. Language
                    SettingsRowItem(
                        icon = Icons.Default.Language,
                        title = stringResource(R.string.choose_language),
                        subtitle = if (activeLang.isAuto) "Auto-detect" else activeLang.nativeName,
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeLang.flag,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        colors = colors,
                        onClick = {
                            onDismiss()
                            onOpenLanguage()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ==========================================
                // SECTION 2: PRIVACY & NETWORK (Grouped Card)
                // ==========================================
                SectionHeader(title = "PRIVACY & SECURITY")

                SettingsGroupCard(colors = colors) {
                    // 2A. Encrypted DNS (DoH) Collapsible
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsRowItem(
                            icon = Icons.Default.Dns,
                            title = stringResource(R.string.section_doh),
                            subtitle = activeDoh.second,
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isDohDropdownExpanded) "Collapse" else "Expand",
                                    tint = colors.textMuted,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(arrowRotation)
                                )
                            },
                            colors = colors,
                            onClick = { isDohDropdownExpanded = !isDohDropdownExpanded }
                        )

                        AnimatedVisibility(
                            visible = isDohDropdownExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surface.copy(alpha = 0.5f))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                dohProviders.forEach { (id, name, desc) ->
                                    val isSelected = selectedDohProvider == id

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(AzokleRadius.sm)
                                            .background(if (isSelected) colors.surfaceElevated else Color.Transparent)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f))
                                            ) {
                                                onDohProviderChange(id)
                                                isDohDropdownExpanded = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = name,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                text = desc,
                                                fontSize = 11.sp,
                                                color = colors.textMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = colors.textPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    SettingsDivider(colors = colors)

                    // 2B. Site Permissions
                    SettingsRowItem(
                        icon = Icons.Default.Security,
                        title = "Site Permissions",
                        subtitle = "Hardware, sensor & storage rules",
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = colors,
                        onClick = { showSiteSettingsSheet = true }
                    )

                    SettingsDivider(colors = colors)

                    // 2C. Background Audio Playback
                    var isBackgroundAudio by remember {
                        mutableStateOf(PreferencesManager(context).isBackgroundAudioEnabled())
                    }
                    SettingsRowItem(
                        icon = Icons.Default.PhoneAndroid,
                        title = "Background Audio",
                        subtitle = "Keep media playing when minimized",
                        trailingContent = {
                            androidx.compose.material3.Switch(
                                checked = isBackgroundAudio,
                                onCheckedChange = { enabled ->
                                    isBackgroundAudio = enabled
                                    PreferencesManager(context).setBackgroundAudioEnabled(enabled)
                                },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                                    checkedTrackColor = colors.actionPrimaryBg,
                                    uncheckedThumbColor = colors.textMuted,
                                    uncheckedTrackColor = colors.surfaceElevated,
                                    uncheckedBorderColor = colors.border
                                )
                            )
                        },
                        colors = colors,
                        onClick = {
                            val newSetting = !isBackgroundAudio
                            isBackgroundAudio = newSetting
                            PreferencesManager(context).setBackgroundAudioEnabled(newSetting)
                        }
                    )

                    SettingsDivider(colors = colors)

                    // 2D. Interactive Tour Guides
                    val effectiveHomeReplay = onReplayHomeTour ?: onReplayTour
                    if (effectiveHomeReplay != null) {
                        SettingsRowItem(
                            icon = Icons.Default.AutoAwesome,
                            title = "Home Screen Tour",
                            subtitle = "Learn about search, shortcuts, and quick controls",
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = colors,
                            onClick = {
                                onDismiss()
                                effectiveHomeReplay()
                            }
                        )

                        SettingsDivider(colors = colors)
                    }

                    if (onReplayTabsTour != null) {
                        SettingsRowItem(
                            icon = Icons.Default.Layers,
                            title = "Tabs Screen Tour",
                            subtitle = "Learn about switching tabs, private modes, and the new tab button",
                            trailingContent = {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = colors,
                            onClick = {
                                onDismiss()
                                onReplayTabsTour()
                            }
                        )

                        SettingsDivider(colors = colors)
                    }

                    // 2D. Clear History
                    SettingsRowItem(
                        icon = Icons.Default.DeleteSweep,
                        title = stringResource(R.string.clear_history),
                        subtitle = "Erase recent search suggestions",
                        trailingContent = null,
                        colors = colors,
                        onClick = {
                            PreferencesManager(context).clearRecentSearches()
                            Toast.makeText(context, "Search history purged", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingsDivider(colors = colors)

                    // 2E. Incinerate Session
                    SettingsRowItem(
                        icon = Icons.Default.LocalFireDepartment,
                        title = stringResource(R.string.incinerate_session),
                        subtitle = "Close tabs, purge cookies and cache",
                        trailingContent = null,
                        colors = colors,
                        onClick = {
                            com.azokle.search.browser.gecko.GeckoEngineManager.clearAllData(context)
                            TabManager.closeAllTabs()
                            Toast.makeText(context, "Browsing session incinerated", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ==========================================
                // SECTION 3: ABOUT & LEGAL (Grouped Card)
                // ==========================================
                SectionHeader(title = "ABOUT")

                SettingsGroupCard(colors = colors) {
                    // 3A. About Us
                    SettingsRowItem(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.about_us),
                        subtitle = "azokle.com/company-info",
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = colors,
                        onClick = {
                            onDismiss()
                            onOpenUrl("https://azokle.com/company-info")
                            onOpenAbout()
                        }
                    )

                    SettingsDivider(colors = colors)

                    // 3B. Privacy Policy
                    SettingsRowItem(
                        icon = Icons.Default.Lock,
                        title = stringResource(R.string.privacy_policy),
                        subtitle = "azokle.com/privacy",
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = colors,
                        onClick = {
                            onDismiss()
                            onOpenUrl("https://azokle.com/privacy")
                            onOpenPrivacyPolicy()
                        }
                    )

                    SettingsDivider(colors = colors)

                    // 3C. Terms of Service
                    SettingsRowItem(
                        icon = Icons.Default.Shield,
                        title = stringResource(R.string.terms_service),
                        subtitle = "azokle.com/terms",
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = colors,
                        onClick = {
                            onDismiss()
                            onOpenUrl("https://azokle.com/terms")
                            onOpenTerms()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Minimal Footer
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "AZOKLE SEARCH • 2026.08",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textMuted,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Zero Telemetry • Sovereign Privacy Engine",
                        fontSize = 10.sp,
                        color = colors.textMuted.copy(alpha = 0.6f)
                    )
                }
            }
        }

        if (showSiteSettingsSheet) {
            SiteSettingsSheet(
                onDismiss = { showSiteSettingsSheet = false }
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = AzokleTheme.colors
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = colors.textMuted,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsGroupCard(
    colors: com.azokle.search.ui.theme.AzokleThemeColors,
    content: @Composable () -> Unit
) {
    val shape = AzokleRadius.lg
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cardBg)
            .border(1.dp, colors.border, shape)
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider(colors: com.azokle.search.ui.theme.AzokleThemeColors) {
    HorizontalDivider(
        color = colors.border.copy(alpha = 0.5f),
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 54.dp)
    )
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    colors: com.azokle.search.ui.theme.AzokleThemeColors,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = colors.textPrimary.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics {
                role = Role.Button
                contentDescription = if (subtitle != null) "$title, $subtitle" else title
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailingContent()
        }
    }
}
